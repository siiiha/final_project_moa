package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

// AI작성: 명시적으로 활성화한 비어 있는 전용 MySQL만 사용한다. 운영 설정·인증 정보는 읽거나 기록하지 않는다.
@EnabledIfEnvironmentVariable(named = "MOA_MEMBER_TEST_DB_ENABLED", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceLock("moa-member-test-database")
class SavingGroupMemberMySqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager transactions;
    SavingGroupService service;
    final List<String> createdTables = new ArrayList<>();

    String required(String key) {
        String value = System.getenv(key);
        assertThat(value).as("필수 테스트 환경 변수 %s", key).isNotBlank();
        return value;
    }

    @BeforeAll void connect() throws Exception {
        String url = required("MOA_MEMBER_TEST_DB_URL");
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(java.util.Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        var source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver"); source.setUrl(url);
        source.setUsername(required("MOA_MEMBER_TEST_DB_USER")); source.setPassword(required("MOA_MEMBER_TEST_DB_PASSWORD"));
        var properties = new Properties(); properties.setProperty("connectTimeout", "5000"); properties.setProperty("socketTimeout", "15000");
        source.setConnectionProperties(properties);
        try (Connection connection = source.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isGreaterThanOrEqualTo(8);
            assertThat(connection.getCatalog()).matches("moa_members_test_[A-Za-z0-9_]+");
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(tables.next()).as("기존 테이블이 없는 전용 테스트 DB만 허용").isFalse();
            }
        }
        jdbc = new JdbcTemplate(source); jdbc.setQueryTimeout(10);
        transactions = new DataSourceTransactionManager(source); transactions.setDefaultTimeout(15);
        String schema = new ClassPathResource("schema.sql").getContentAsString(StandardCharsets.UTF_8);
        try {
            for (String table : List.of("members", "groups", "group_member", "saving_group")) {
                var ddl = Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS `" + table + "`\\s*\\(.*?\\)\\s*ENGINE=[^;]+;").matcher(schema);
                assertThat(ddl.find()).isTrue(); jdbc.execute(ddl.group()); createdTables.add(table);
            }
            var factory = new SqlSessionFactoryBean(); factory.setDataSource(source);
            var configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true); configuration.setDefaultStatementTimeout(10);
            configuration.getTypeAliasRegistry().registerAliases("com.nightstudy.moa.savingGroup.dto");
            factory.setConfiguration(configuration); factory.setMapperLocations(new ClassPathResource("mappers/SavingGroupMapper.xml"));
            var template = new SqlSessionTemplate(factory.getObject());
            var proxy = new ProxyFactory(new SavingGroupServiceImpl(template.getMapper(SavingGroupMapper.class)));
            proxy.setInterfaces(SavingGroupService.class);
            proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
            service = (SavingGroupService) proxy.getProxy();
        } catch (Exception | AssertionError failure) { cleanup(); throw failure; }
    }

    @BeforeEach void fixtures() {
        // AI작성: 이 테스트가 빈 전용 DB에 생성한 네 테이블만 초기화하며 합성 회원만 저장한다.
        jdbc.update("DELETE FROM group_member"); jdbc.update("DELETE FROM saving_group");
        jdbc.update("DELETE FROM `groups`"); jdbc.update("DELETE FROM members");
        jdbc.update("INSERT INTO `groups` (group_id, region_id, member_id, group_name) VALUES (20, 1, 1, 'members_test')");
        jdbc.update("INSERT INTO saving_group (saving_group_id, group_id, status) VALUES (10, 20, '02')");
        for (long id = 1; id <= 26; id++) {
            jdbc.update("INSERT INTO members (member_id, name, nickname, email) VALUES (?, ?, ?, ?)",
                    id, "합성계원" + id, "test_" + id, "test" + id + "@example.com");
            jdbc.update("INSERT INTO group_member (member_id, group_id, member_role, membership_status, joined_at) "
                    + "VALUES (?, 20, ?, 'APPROVED', '2026-11-05 10:00:00')", id, id == 1 ? "OWNER" : "MEMBER");
        }
    }

    @Test void actualSqlReturnsTwentyWithUnmaskedNamesAndMaskedEmail() {
        var first = service.getSavingGroupMembers(10L, 1L, 1);
        var second = service.getSavingGroupMembers(10L, 1L, 2);
        assertThat(first.getPageMeta().getTotal()).isEqualTo(20);
        assertThat(first.getItems()).hasSize(10); assertThat(second.getItems()).hasSize(10);
        assertThat(first.getItems().get(0).getMemberId()).isEqualTo("26");
        assertThat(second.getItems().get(9).getMemberId()).isEqualTo("7");
        assertThat(first.getItems().get(0).getName()).isEqualTo("합성계원26");
        assertThat(first.getItems().get(0).getEmail()).isEqualTo("t***@example.com");
        assertThat(first.getItems().get(0).getJoinedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 11, 5, 10, 0));
        assertThat(service.getSavingGroupMembers(10L, 1L, 3).getItems()).isEmpty();
    }

    @Test @Timeout(40) void concurrentAssignmentsHaveExactlyOneManager() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (long id : new long[]{2L, 3L}) results.add(pool.submit(() -> {
                ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("동시 시작 실패");
                try { service.updateSavingGroupMemberRole(10L, id, 1L, "MANAGER"); return "OK"; }
                catch (ApiException failure) { return failure.getCode(); }
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(results.get(0).get(20, TimeUnit.SECONDS), results.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "SAVING_GROUP_MANAGER_ALREADY_EXISTS");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_member WHERE group_id=20 AND member_role='MANAGER' AND membership_status='APPROVED'", Integer.class)).isEqualTo(1);
        } finally { start.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue(); }
    }

    @Test void outerRollbackRestoresRoleAndOwnerCannotBeChanged() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            service.updateSavingGroupMemberRole(10L, 2L, 1L, "MANAGER");
            throw new IllegalStateException("테스트 롤백");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT member_role FROM group_member WHERE group_id=20 AND member_id=2", String.class)).isEqualTo("MEMBER");
        assertThatThrownBy(() -> service.updateSavingGroupMemberRole(10L, 1L, 1L, "MEMBER"))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.getCode()).isEqualTo("SAVING_GROUP_OWNER_ROLE_CHANGE_NOT_ALLOWED"));
    }

    @AfterAll void cleanup() {
        // AI작성: 최초 빈 DB 검증 이후 이 클래스에서 생성했다고 기록한 테이블만 역순으로 정리한다.
        for (int index = createdTables.size() - 1; index >= 0; index--) jdbc.execute("DROP TABLE `" + createdTables.get(index) + "`");
        createdTables.clear();
    }
}
