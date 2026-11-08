package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.notification.service.NotificationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import com.nightstudy.moa.smallGroup.SmallGroupServiceImpl;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

// AI작성: 외부 서비스 없이 실제 MySQL·Mapper·트랜잭션 프록시를 사용한다.
// 소모임 테스트도 같은 시나리오와 준비·정리 절차를 상속해 중복 구현을 피한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("moa-join-test-database")
public class SavingGroupJoinConcurrencyIntegrationTest {
    private static final long GROUP_ID = 20L;
    private static final long DOMAIN_GROUP_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final int WORKERS = 8;
    private final List<String> createdTables = new ArrayList<>();
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactionManager;
    private SavingGroupService savingService;
    private SmallGroupService smallService;

    // AI작성: 실행할 도메인만 바꾸고 모든 경쟁 조건을 두 모임에 똑같이 적용한다.
    protected boolean isSavingGroup() {
        return true;
    }

    @BeforeAll
    protected void connectToEmptyTestDatabase() throws Exception {
        String url = requiredEnvironment("MOA_TEST_DB_URL");
        // AI작성: 일반 프로젝트 DB와 URL에 포함된 인증 정보를 사용하지 않는다.
        assertThat(url).startsWith("jdbc:mysql://");
        assertThat(url).doesNotContain("@");
        assertThat(url.toLowerCase(java.util.Locale.ROOT))
                .doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        DriverManagerDataSource dataSource = new RepeatableReadDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(requiredEnvironment("MOA_TEST_DB_USER"));
        dataSource.setPassword(requiredEnvironment("MOA_TEST_DB_PASSWORD"));
        Properties connectionProperties = new Properties();
        connectionProperties.setProperty("connectTimeout", "5000");
        connectionProperties.setProperty("socketTimeout", "15000");
        dataSource.setConnectionProperties(connectionProperties);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isGreaterThanOrEqualTo(8);
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (ResultSet tables = connection.getMetaData().getTables(
                    connection.getCatalog(), null, "%", new String[] { "TABLE", "VIEW" })) {
                assertThat(tables.next()).as("기존 테이블이 없는 테스트 전용 DB만 허용합니다.").isFalse();
            }
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(10);
        transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setDefaultTimeout(15);

        String domainTable = isSavingGroup() ? "saving_group" : "small_group";
        String schema = new ClassPathResource("schema.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        try {
            for (String table : List.of("members", "groups", "group_member", domainTable)) {
                Matcher ddl = Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS `" + table
                        + "`\\s*\\(.*?\\)\\s*ENGINE=[^;]+;").matcher(schema);
                assertThat(ddl.find()).as("schema.sql의 %s 정의", table).isTrue();
                jdbc.execute(ddl.group());
                createdTables.add(table);
            }

            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(dataSource);
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setDefaultStatementTimeout(10);
            configuration.getTypeAliasRegistry().registerAliases("com.nightstudy.moa.savingGroup.dto");
            factoryBean.setConfiguration(configuration);
            factoryBean.setMapperLocations(new ClassPathResource(
                    "mappers/" + (isSavingGroup() ? "SavingGroup" : "SmallGroup") + "Mapper.xml"));
            SqlSessionFactory factory = factoryBean.getObject();
            SqlSessionTemplate template = new SqlSessionTemplate(factory);
            if (isSavingGroup()) {
                savingService = transactionalProxy(new SavingGroupServiceImpl(
                        template.getMapper(SavingGroupMapper.class),
                        org.mockito.Mockito.mock(com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper.class),
                        new AccountNumberCipher(new com.nightstudy.moa.common.config.AccountEncryptionProperties()),
                        org.mockito.Mockito.mock(NotificationService.class)), SavingGroupService.class);
                assertThat(AopUtils.isAopProxy(savingService)).isTrue();
            } else {
                smallService = transactionalProxy(new SmallGroupServiceImpl(
                        template.getMapper(SmallGroupMapper.class),
                        org.mockito.Mockito.mock(NotificationService.class)), SmallGroupService.class);
                assertThat(AopUtils.isAopProxy(smallService)).isTrue();
            }
        } catch (Exception | AssertionError failure) {
            dropCreatedTables();
            throw failure;
        }
    }

    @BeforeEach
    protected void insertSyntheticFixtures() {
        // AI작성: 비어 있는 전용 DB에 이 클래스가 생성한 테이블만 초기화한다.
        jdbc.update("DELETE FROM group_member");
        jdbc.update("DELETE FROM " + (isSavingGroup() ? "saving_group" : "small_group"));
        jdbc.update("DELETE FROM `groups`");
        jdbc.update("DELETE FROM members");
        for (long memberId = OWNER_ID; memberId <= WORKERS + 1; memberId++) {
            jdbc.update("INSERT INTO members (member_id, name, nickname, identity_verified_at) "
                    + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)", memberId,
                    "test_member_" + memberId, "test_nickname_" + memberId);
        }
        jdbc.update("INSERT INTO `groups` (group_id, region_id, member_id, group_name, max_members) "
                + "VALUES (?, 1, ?, 'join_concurrency_test', 20)", GROUP_ID, OWNER_ID);
        if (isSavingGroup()) {
            jdbc.update("INSERT INTO saving_group (saving_group_id, group_id, status) VALUES (?, ?, '01')",
                    DOMAIN_GROUP_ID, GROUP_ID);
        } else {
            jdbc.update("INSERT INTO small_group (small_group_id, group_id, meet_date) VALUES (?, ?, ?)",
                    DOMAIN_GROUP_ID, GROUP_ID, java.sql.Date.valueOf(
                            LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1)));
        }
        jdbc.update("INSERT INTO group_member (member_id, group_id, member_role, membership_status) "
                + "VALUES (?, ?, 'OWNER', 'APPROVED')", OWNER_ID, GROUP_ID);
    }

    @Test
    @Timeout(40)
    protected void concurrentRequestsCreateOnlyOnePendingMembership() throws Exception {
        List<String> outcomes = race(java.util.Collections.nCopies(WORKERS, () -> request(2L)));
        assertSingleWinner(outcomes, "JOIN_REQUEST_ALREADY_PENDING", "JOIN_REQUEST_DUPLICATED");
        assertThat(membershipCount(2L)).isEqualTo(1);
        assertThat(membershipStatus(2L)).isEqualTo("PENDING");
        assertThat(approvedCount()).isEqualTo(1);
    }

    @Test
    @Timeout(40)
    protected void concurrentReapplicationsReuseTheOriginalLeftMembership() throws Exception {
        insertMembership(2L, "LEFT");
        long originalId = requestId(2L);
        jdbc.update("UPDATE group_member SET approved_by = ?, approved_at = CURRENT_TIMESTAMP, "
                + "joined_at = CURRENT_TIMESTAMP, left_at = CURRENT_TIMESTAMP, application_message = 'old_test' "
                + "WHERE crew_member_id = ?", OWNER_ID, originalId);
        List<String> outcomes = race(java.util.Collections.nCopies(WORKERS, () -> request(2L)));
        assertSingleWinner(outcomes, "JOIN_REQUEST_ALREADY_PENDING", "JOIN_REQUEST_DUPLICATED");
        assertThat(requestId(2L)).isEqualTo(originalId);
        assertThat(membershipCount(2L)).isEqualTo(1);
        assertThat(membershipStatus(2L)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE crew_member_id = ? "
                + "AND approved_by IS NULL AND approved_at IS NULL AND joined_at IS NULL "
                + "AND left_at IS NULL AND application_message IS NULL", Long.class, originalId)).isEqualTo(1L);
    }

    @Test
    @Timeout(40)
    protected void concurrentApprovalsNeverExceedTheLastAvailablePlace() throws Exception {
        jdbc.update("UPDATE `groups` SET max_members = 2 WHERE group_id = ?", GROUP_ID);
        List<Runnable> actions = new ArrayList<>();
        for (long memberId = 2; memberId <= WORKERS + 1; memberId++) {
            insertMembership(memberId, "PENDING");
            long requestId = requestId(memberId);
            actions.add(() -> approve(requestId));
        }
        List<String> outcomes = race(actions);
        assertSingleWinner(outcomes, isSavingGroup() ? "SAVING_GROUP_CAPACITY_FULL" : "SMALL_GROUP_CAPACITY_FULL");
        assertThat(approvedCount()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id = ? "
                + "AND membership_status = 'PENDING'", Long.class, GROUP_ID)).isEqualTo((long) WORKERS - 1);
    }

    @Test
    @Timeout(40)
    protected void approvalAndRejectionOfTheSameRequestHaveOneWinner() throws Exception {
        insertMembership(2L, "PENDING");
        long id = requestId(2L);
        List<String> outcomes = race(List.of(() -> approve(id), () -> reject(id)));
        assertSingleWinner(outcomes, "JOIN_REQUEST_NOT_PENDING");
        String finalStatus = membershipStatus(2L);
        assertThat(finalStatus).isIn("APPROVED", "REJECTED");
        assertThat(approvedCount()).isEqualTo("APPROVED".equals(finalStatus) ? 2L : 1L);
        assertThat(membershipCount(2L)).isEqualTo(1);
    }

    @Test
    protected void repeatedRequestsAndApprovalsDoNotCreateExtraRows() {
        request(2L);
        assertThat(outcome(() -> request(2L))).isEqualTo("JOIN_REQUEST_ALREADY_PENDING");
        long id = requestId(2L);
        approve(id);
        assertThat(outcome(() -> approve(id))).isEqualTo("JOIN_REQUEST_NOT_PENDING");
        assertThat(outcome(() -> request(2L))).isEqualTo(
                isSavingGroup() ? "ALREADY_JOINED_SAVING_GROUP" : "ALREADY_JOINED_SMALL_GROUP");
        assertThat(membershipCount(2L)).isEqualTo(1);
        assertThat(approvedCount()).isEqualTo(2);
    }

    @Test
    protected void databaseUniqueConstraintRejectsDuplicateMembershipDirectly() {
        insertMembership(2L, "PENDING");
        assertThatThrownBy(() -> insertMembership(2L, "PENDING")).isInstanceOf(DuplicateKeyException.class);
        assertThat(membershipCount(2L)).isEqualTo(1);
    }

    @Test
    @Timeout(40)
    protected void competingRequestWaitsForTheFirstTransactionToCommit() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inserted = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        CountDownLatch competing = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
                transaction.executeWithoutResult(status -> {
                    request(2L);
                    inserted.countDown();
                    await(commit);
                });
            });
            assertThat(inserted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> second = executor.submit(() -> {
                competing.countDown();
                return outcome(() -> request(2L));
            });
            assertThat(competing.await(5, TimeUnit.SECONDS)).isTrue();
            // AI작성: 첫 트랜잭션을 보류해 두 번째 요청이 미커밋 상태를 우회하지 못하는지 확인한다.
            assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            commit.countDown();
            first.get(20, TimeUnit.SECONDS);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo("JOIN_REQUEST_ALREADY_PENDING");
            assertThat(membershipCount(2L)).isEqualTo(1);
        } finally {
            commit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    // AI작성: 작업 시작을 맞추고 업무 예외만 결과로 수집한다. 데드락·SQL 오류·시간 초과는 테스트 실패다.
    private List<String> race(List<Runnable> actions) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(actions.size());
        CountDownLatch ready = new CountDownLatch(actions.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (Runnable action : actions) {
                futures.add(executor.submit((Callable<String>) () -> {
                    ready.countDown();
                    await(start);
                    return outcome(action);
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                long remaining = deadline - System.nanoTime();
                assertThat(remaining).isPositive();
                outcomes.add(future.get(remaining, TimeUnit.NANOSECONDS));
            }
            return outcomes;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void assertSingleWinner(List<String> outcomes, String... allowedFailures) {
        assertThat(outcomes.stream().filter("OK"::equals).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(result -> !"OK".equals(result)).toList())
                .containsOnly(allowedFailures);
    }

    private String outcome(Runnable action) {
        try {
            action.run();
            return "OK";
        } catch (ApiException failure) {
            assertThat(failure.getStatus().is4xxClientError()).isTrue();
            return failure.getCode();
        }
    }

    private void request(long memberId) {
        if (isSavingGroup()) savingService.requestSavingGroupJoin(DOMAIN_GROUP_ID, memberId);
        else smallService.requestSmallGroupJoin(DOMAIN_GROUP_ID, memberId);
    }

    private void approve(long requestId) {
        if (isSavingGroup()) savingService.approveSavingGroupJoin(DOMAIN_GROUP_ID, requestId, OWNER_ID);
        else smallService.approveSmallGroupJoin(DOMAIN_GROUP_ID, requestId, OWNER_ID);
    }

    private void reject(long requestId) {
        if (isSavingGroup()) savingService.rejectSavingGroupJoin(DOMAIN_GROUP_ID, requestId, OWNER_ID);
        else smallService.rejectSmallGroupJoin(DOMAIN_GROUP_ID, requestId, OWNER_ID);
    }

    private void insertMembership(long memberId, String status) {
        jdbc.update("INSERT INTO group_member (member_id, group_id, membership_status) VALUES (?, ?, ?)",
                memberId, GROUP_ID, status);
    }

    private long requestId(long memberId) {
        return jdbc.queryForObject("SELECT crew_member_id FROM group_member WHERE group_id = ? AND member_id = ?",
                Long.class, GROUP_ID, memberId);
    }

    private long membershipCount(long memberId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id = ? AND member_id = ?",
                Long.class, GROUP_ID, memberId);
    }

    private String membershipStatus(long memberId) {
        return jdbc.queryForObject("SELECT membership_status FROM group_member WHERE group_id = ? AND member_id = ?",
                String.class, GROUP_ID, memberId);
    }

    private long approvedCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id = ? AND membership_status = 'APPROVED'",
                Long.class, GROUP_ID);
    }

    // AI작성: 모든 작업 연결을 동일한 격리 수준으로 열고 잠금 대기를 제한한다.
    private static class RepeatableReadDataSource extends DriverManagerDataSource {
        @Override
        public Connection getConnection() throws java.sql.SQLException {
            return prepare(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws java.sql.SQLException {
            return prepare(super.getConnection(username, password));
        }

        private Connection prepare(Connection connection) throws java.sql.SQLException {
            try {
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                try (java.sql.Statement statement = connection.createStatement()) {
                    statement.execute("SET SESSION innodb_lock_wait_timeout = 5");
                }
                return connection;
            } catch (java.sql.SQLException failure) {
                connection.close();
                throw failure;
            }
        }
    }

    private <T> T transactionalProxy(T target, Class<T> contract) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setInterfaces(contract);
        factory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return contract.cast(factory.getProxy());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("동시 요청 동기화 시간 초과");
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 요청 검증 중단", failure);
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        assertThat(value).as("실행 환경의 %s 설정이 필요합니다.", name).isNotBlank();
        return value;
    }

    @AfterAll
    protected void dropCreatedTables() {
        // AI작성: 시작 시 비어 있음을 확인했고 생성 성공을 기록한 테이블만 역순으로 정리한다.
        for (int index = createdTables.size() - 1; index >= 0; index--) {
            jdbc.execute("DROP TABLE `" + createdTables.get(index) + "`");
            createdTables.remove(index);
        }
    }
}
