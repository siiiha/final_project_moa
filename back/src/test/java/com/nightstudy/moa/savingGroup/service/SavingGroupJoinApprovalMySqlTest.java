package com.nightstudy.moa.savingGroup.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

// AI작성: 별도 테스트 서버에서만 명시적으로 활성화한다. 기존 DB 테이블에는 접근하지 않는다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_MYSQL_ALLOW_SCHEMA_CREATE", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SavingGroupJoinApprovalMySqlTest {
    private final String schema = "moa_approval_test_" + UUID.randomUUID().toString().replace("-", "");
    private String adminUrl;
    private String user;
    private String password;
    private boolean created;
    private JdbcTemplate jdbc;
    private SavingGroupMapper mapper;
    private SavingGroupService service;
    private TransactionTemplate transaction;

    @BeforeAll
    void openIsolatedSchema() throws Exception {
        adminUrl = System.getenv("MOA_TEST_MYSQL_URL");
        user = System.getenv("MOA_TEST_MYSQL_USER");
        password = System.getenv("MOA_TEST_MYSQL_PASSWORD");
        assumeTrue(adminUrl != null && user != null && password != null, "별도 MySQL 테스트 접속 설정 필요");
        URI address = URI.create(adminUrl.substring("jdbc:".length()));
        assertEquals("mysql", address.getScheme());
        // AI작성: CREATE 성공한 임의 이름만 정리 대상으로 기록한다. 공용 DB URL을 설정하면 안 된다.
        try (var connection = DriverManager.getConnection(adminUrl, user, password);
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            created = true;
        }
        String url = "jdbc:mysql://" + address.getRawAuthority() + "/" + schema
                + (address.getRawQuery() == null ? "" : "?" + address.getRawQuery());
        var source = new DriverManagerDataSource(url, user, password);
        jdbc = new JdbcTemplate(source);
        // AI작성: 조회에 필요한 합성 데이터용 최소 테이블이며 실제 개인·금융 정보는 사용하지 않는다.
        jdbc.execute("CREATE TABLE members (member_id BIGINT PRIMARY KEY, account_status VARCHAR(20) DEFAULT 'ACTIVE', identity_verified_at DATETIME DEFAULT CURRENT_TIMESTAMP, deleted_at DATETIME NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE `groups` (group_id BIGINT PRIMARY KEY, member_id BIGINT NOT NULL, max_members INT NULL, deleted_at DATETIME NULL) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE saving_group (saving_group_id BIGINT PRIMARY KEY, group_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL, KEY(group_id)) ENGINE=InnoDB");
        try (var stream = new ClassPathResource("schema.sql").getInputStream()) {
            String ddl = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var table = java.util.regex.Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS `group_member`.*?;").matcher(ddl);
            assertTrue(table.find());
            jdbc.execute(table.group());
        }
        jdbc.execute("ALTER TABLE group_member ADD FOREIGN KEY (approved_by) REFERENCES members(member_id)");
        var configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.getTypeAliasRegistry().registerAlias("SavingGroupCreationRequest",
                com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest.class);
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(source);
        factory.setConfiguration(configuration);
        factory.setMapperLocations(new ClassPathResource("mappers/SavingGroupMapper.xml"));
        mapper = new SqlSessionTemplate(factory.getObject()).getMapper(SavingGroupMapper.class);
        var manager = new DataSourceTransactionManager(source);
        transaction = new TransactionTemplate(manager);
        transaction.setTimeout(15);
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(new SavingGroupServiceImpl(mapper));
        proxy.addAdvice(interceptor);
        service = (SavingGroupService) proxy.getProxy();
    }

    @AfterAll
    void closeIsolatedSchema() throws Exception {
        // AI작성: 이 테스트가 CREATE에 성공한 고유 스키마만 삭제한다.
        if (created && schema.matches("moa_approval_test_[a-f0-9]{32}")) {
            try (var connection = DriverManager.getConnection(adminUrl, user, password);
                 var statement = connection.createStatement()) {
                statement.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM group_member");
        jdbc.update("DELETE FROM saving_group");
        jdbc.update("DELETE FROM `groups`");
        jdbc.update("DELETE FROM members");
        for (long id : new long[] {20, 21, 40}) jdbc.update("INSERT INTO members(member_id) VALUES (?)", id);
        group(10, 30, 5, "01");
        relation(1, 40, 30, "OWNER", "APPROVED");
        relation(50, 20, 30, "MEMBER", "PENDING");
    }

    @Test
    void 기존행승인저장값과중복방지() {
        int before = count("SELECT COUNT(*) FROM group_member");
        var requestedAt = jdbc.queryForMap("SELECT requested_at FROM group_member WHERE crew_member_id=50").get("requested_at");
        service.approveSavingGroupJoin(10L, 50L, 40L);
        var row = jdbc.queryForMap("SELECT * FROM group_member WHERE crew_member_id=50");
        assertEquals("APPROVED", row.get("membership_status"));
        assertEquals("MEMBER", row.get("member_role"));
        assertEquals(40L, ((Number) row.get("approved_by")).longValue());
        assertNotNull(row.get("approved_at"));
        assertEquals(row.get("joined_at"), row.get("approved_at"));
        assertEquals(requestedAt, row.get("requested_at"));
        assertEquals(before, count("SELECT COUNT(*) FROM group_member"));
        assertEquals("JOIN_REQUEST_NOT_PENDING", assertThrows(ApiException.class,
                () -> service.approveSavingGroupJoin(10L, 50L, 40L)).getCode());
    }

    @Test
    void UPDATE조건불일치시영향행수는영이다() {
        transaction.executeWithoutResult(status -> {
            assertEquals(0, mapper.approveSavingGroupJoinRequest(50L, 999L, 40L));
            assertEquals(0, mapper.approveSavingGroupJoinRequest(1L, 30L, 40L));
        });
        assertEquals("PENDING", jdbc.queryForObject("SELECT membership_status FROM group_member WHERE crew_member_id=50", String.class));
    }

    @Test
    void 승인뒤실패하면실제저장값도롤백된다() {
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            service.approveSavingGroupJoin(10L, 50L, 40L);
            throw new IllegalStateException("테스트용 후속 실패");
        }));
        var row = jdbc.queryForMap("SELECT * FROM group_member WHERE crew_member_id=50");
        assertEquals("PENDING", row.get("membership_status"));
        assertNull(row.get("approved_by"));
        assertNull(row.get("approved_at"));
        assertNull(row.get("joined_at"));
    }

    @Test
    void 재신청은이전승인정보를초기화한다() {
        jdbc.update("UPDATE group_member SET membership_status='LEFT', approved_by=40, approved_at=CURRENT_TIMESTAMP, joined_at=CURRENT_TIMESTAMP, left_at=CURRENT_TIMESTAMP WHERE crew_member_id=50");
        service.requestSavingGroupJoin(10L, 20L);
        var row = jdbc.queryForMap("SELECT * FROM group_member WHERE crew_member_id=50");
        assertEquals("PENDING", row.get("membership_status"));
        for (String column : new String[] {"approved_by", "approved_at", "joined_at", "left_at"}) assertNull(row.get(column));
    }

    @Test
    void 같은신청동시승인은한번만성공한다() throws Exception {
        assertEquals(1, race(() -> service.approveSavingGroupJoin(10L, 50L, 40L),
                () -> service.approveSavingGroupJoin(10L, 50L, 40L)));
        assertEquals(2, count("SELECT COUNT(*) FROM group_member"));
    }

    @Test
    void 같은모임동시승인도계주포함정원을넘지않는다() throws Exception {
        jdbc.update("UPDATE `groups` SET max_members=2 WHERE group_id=30");
        relation(51, 21, 30, "MEMBER", "PENDING");
        assertEquals(1, race(() -> service.approveSavingGroupJoin(10L, 50L, 40L),
                () -> service.approveSavingGroupJoin(10L, 51L, 40L)));
        assertEquals(2, count("SELECT COUNT(*) FROM group_member WHERE group_id=30 AND membership_status='APPROVED'"));
        assertEquals(3, count("SELECT COUNT(*) FROM group_member"));
    }

    @Test
    void 같은회원의여러모임동시승인도가입다섯개를넘지않는다() throws Exception {
        for (int index = 0; index < 4; index++) {
            group(100 + index, 200 + index, 5, "0" + index);
            relation(300 + index, 20, 200 + index, "MEMBER", "APPROVED");
        }
        group(11, 31, 5, "01");
        relation(2, 40, 31, "OWNER", "APPROVED");
        relation(51, 20, 31, "MEMBER", "PENDING");
        assertEquals(1, race(() -> service.approveSavingGroupJoin(10L, 50L, 40L),
                () -> service.approveSavingGroupJoin(11L, 51L, 40L)));
        assertEquals(5, count("SELECT COUNT(*) FROM group_member WHERE member_id=20 AND membership_status='APPROVED'"));
    }

    // AI작성: 동시에 시작한 두 실제 트랜잭션을 기다린다. 교착 피해자는 롤백 실패 응답으로 허용한다.
    private int race(Runnable first, Runnable second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var one = pool.submit(atStart(first, ready, start));
            var two = pool.submit(atStart(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            return one.get(25, TimeUnit.SECONDS) + two.get(25, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private Callable<Integer> atStart(Runnable action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("동시 실행 준비 시간 초과");
            try {
                transaction.executeWithoutResult(status -> action.run());
                return 1;
            } catch (ApiException error) {
                assertTrue(java.util.Set.of("JOIN_REQUEST_NOT_PENDING", "SAVING_GROUP_CAPACITY_FULL",
                        "SAVING_GROUP_JOIN_LIMIT_EXCEEDED").contains(error.getCode()));
                return 0;
            } catch (TransientDataAccessException error) {
                // AI작성: 교착 피해자의 실패는 허용하되, 문법·매핑 등 비일시적 DB 오류는 테스트를 실패시킨다.
                return 0;
            }
        };
    }

    private void group(long savingGroupId, long groupId, int capacity, String status) {
        jdbc.update("INSERT INTO `groups`(group_id,member_id,max_members) VALUES (?,40,?)", groupId, capacity);
        jdbc.update("INSERT INTO saving_group(saving_group_id,group_id,status) VALUES (?,?,?)", savingGroupId, groupId, status);
    }

    private void relation(long id, long memberId, long groupId, String role, String status) {
        jdbc.update("INSERT INTO group_member(crew_member_id,member_id,group_id,member_role,membership_status) VALUES (?,?,?,?,?)", id, memberId, groupId, role, status);
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
