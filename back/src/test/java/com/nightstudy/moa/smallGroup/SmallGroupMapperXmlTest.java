package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteCreateRequest;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// AI작성: 소모임 설정과 가입 신청용 MyBatis statement가 애플리케이션 기동 전 파싱되는지 확인한다.
class SmallGroupMapperXmlTest {

    private static final String NAMESPACE = "com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper.";

    // AI작성: 종료 Mapper 계약이 별도 설정 변경 없이 UPDATE로 파싱되고 두 ID를 바인딩하는지 검증한다.
    @Test
    void parsesCloseSmallGroupVoteStatement() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mappers/SmallGroupMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var statement = configuration.getMappedStatement(NAMESPACE + "closeSmallGroupVote");
        assertThat(statement.getSqlCommandType()).isEqualTo(org.apache.ibatis.mapping.SqlCommandType.UPDATE);
        assertThat(statement.getBoundSql(Map.of("smallGroupId", 10L, "smallGroupVoteId", 1L))
                .getParameterMappings()).extracting(mapping -> mapping.getProperty())
                .containsExactly("smallGroupVoteId", "smallGroupId");
        assertThat(configuration.hasMapper(SmallGroupMapper.class)).isTrue();
    }

    // AI작성: 마감 전·마감 미설정 투표를 닫아도 ends_at을 보존하고 재요청으로 종료 시각이 밀리지 않는다.
    @Test
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_JDBC_URL", matches = ".+")
    void closeSmallGroupVoteRecordsClosedAtAndPreservesEndsAtInMysql() throws Exception {
        try (Connection connection = openVoteTestConnection()) {
            insertVote(connection, 1, 10, "OPEN", null, null, false);
            insertVote(connection, 2, 10, "OPEN", "2026-11-03T16:00:00", null, false);
            assertThat(openVoteCount(connection, 10)).isEqualTo(2L);
            for (long voteId : new long[] {1L, 2L}) {
                try (PreparedStatement update = voteQuery(connection, "closeSmallGroupVote",
                        Map.of("smallGroupId", 10L, "smallGroupVoteId", voteId))) {
                    assertThat(update.executeUpdate()).isEqualTo(1);
                }
            }
            assertThat(openVoteCount(connection, 10)).isZero();
            try (var statement = connection.createStatement()) {
                statement.execute("SET timestamp = UNIX_TIMESTAMP('2026-11-03 15:30:00')");
            }
            for (long voteId : new long[] {1L, 2L}) {
                try (PreparedStatement update = voteQuery(connection, "closeSmallGroupVote",
                        Map.of("smallGroupId", 10L, "smallGroupVoteId", voteId))) {
                    assertThat(update.executeUpdate()).isZero();
                }
            }
            try (var statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT * FROM small_group_vote ORDER BY small_group_vote_id")) {
                for (long voteId : new long[] {1L, 2L}) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong("small_group_vote_id")).isEqualTo(voteId);
                    assertThat(rows.getString("status")).isEqualTo("CLOSED");
                    assertThat(rows.getTimestamp("closed_at").toLocalDateTime())
                            .isEqualTo(LocalDateTime.of(2026, 11, 3, 15, 0));
                    assertThat(rows.getTimestamp("ends_at"))
                            .isEqualTo(voteId == 1L ? null : Timestamp.valueOf("2026-11-03 16:00:00"));
                    assertThat(rows.getTimestamp("deleted_at")).isNull();
                    assertThat(rows.getString("title")).isEqualTo("검증용 투표");
                }
                assertThat(rows.next()).isFalse();
            }
        }
    }

    // AI작성: 마감 정각·경과·종료·삭제·다른 소모임·없는 투표를 수정하거나 기존 closed_at을 덮어쓰지 않는다.
    @Test
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_JDBC_URL", matches = ".+")
    void closeSmallGroupVoteExcludesIneligibleTargetsInMysql() throws Exception {
        try (Connection connection = openVoteTestConnection()) {
            insertVote(connection, 1, 10, "OPEN", "2026-11-03T15:00:00", null, false);
            insertVote(connection, 2, 10, "OPEN", "2026-11-03T14:59:59", null, false);
            insertVote(connection, 3, 10, "CLOSED", null, "2026-11-02T15:00:00", false);
            insertVote(connection, 4, 10, "CLOSED", null, null, false);
            insertVote(connection, 5, 10, "OPEN", null, null, true);
            insertVote(connection, 6, 99, "OPEN", null, null, false);
            insertVote(connection, 7, 10, "OPEN", null, "2026-11-03T14:00:00", false);
            List<String> before = closeVoteTargetSnapshot(connection);
            for (long voteId = 1; voteId <= 8; voteId++) {
                try (PreparedStatement update = voteQuery(connection, "closeSmallGroupVote",
                        Map.of("smallGroupId", 10L, "smallGroupVoteId", voteId))) {
                    assertThat(update.executeUpdate()).as("투표 ID %s 갱신 제외", voteId).isZero();
                }
            }
            assertThat(closeVoteTargetSnapshot(connection)).containsExactlyElementsOf(before);
        }
    }

    // AI작성: 제외 대상의 전체 행을 비교해 상태뿐 아니라 기존 날짜·삭제 여부도 변경되지 않았는지 확인한다.
    private List<String> closeVoteTargetSnapshot(Connection connection) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (var statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM small_group_vote ORDER BY small_group_vote_id")) {
            while (result.next()) {
                List<String> columns = new ArrayList<>();
                for (int index = 1; index <= result.getMetaData().getColumnCount(); index++) {
                    columns.add(result.getString(index));
                }
                rows.add(String.join("|", columns));
            }
        }
        return rows;
    }

    // AI작성: 별도 임시 스키마에서 두 연결이 실제 Service·Mapper로 동시에 생성해도 다섯 개를 넘지 않는지 검증한다.
    // 스키마 생성 권한이 있는 테스트 전용 MySQL에서만 추가 환경 변수로 명시적으로 실행한다.
    @Test
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_CONCURRENCY", matches = "true")
    void concurrentVoteCreationAllowsOnlyOneFifthVoteInMysql() throws Exception {
        String schema = "moa_vote_concurrency_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = openVoteTestConnection(); var statement = admin.createStatement()) {
            statement.execute("CREATE DATABASE `" + schema + "`");
            var workers = Executors.newFixedThreadPool(2);
            try {
                statement.execute("CREATE TABLE `" + schema + "`.`groups` (group_id BIGINT PRIMARY KEY, "
                        + "member_id BIGINT, max_members INT, deleted_at DATETIME) ENGINE=InnoDB");
                statement.execute("CREATE TABLE `" + schema + "`.small_group (small_group_id BIGINT PRIMARY KEY, "
                        + "group_id BIGINT) ENGINE=InnoDB");
                statement.execute("CREATE TABLE `" + schema + "`.group_member (group_id BIGINT, member_id BIGINT, "
                        + "member_role VARCHAR(20), membership_status VARCHAR(20), PRIMARY KEY(group_id, member_id)) ENGINE=InnoDB");
                statement.execute("CREATE TABLE `" + schema + "`.small_group_vote (small_group_vote_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "small_group_id BIGINT, creator_member_id BIGINT, title VARCHAR(100), is_multiple_choice BOOLEAN, "
                        + "is_anonymous BOOLEAN, ends_at DATETIME, status VARCHAR(20), closed_at DATETIME, deleted_at DATETIME) ENGINE=InnoDB");
                statement.execute("CREATE TABLE `" + schema + "`.small_group_vote_option (small_group_vote_id BIGINT, "
                        + "option_content VARCHAR(100), display_order INT) ENGINE=InnoDB");
                statement.execute("INSERT INTO `" + schema + "`.`groups` VALUES (20, 40, 10, NULL)");
                statement.execute("INSERT INTO `" + schema + "`.small_group VALUES (10, 20)");
                statement.execute("INSERT INTO `" + schema + "`.group_member VALUES (20, 40, 'OWNER', 'APPROVED')");
                statement.execute("INSERT INTO `" + schema + "`.small_group_vote (small_group_id, status) "
                        + "VALUES (10, 'OPEN'), (10, 'OPEN'), (10, 'OPEN'), (10, 'OPEN')");
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                var first = workers.submit(() -> createVoteInTransaction(schema, ready, start));
                var second = workers.submit(() -> createVoteInTransaction(schema, ready, start));
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                        .containsExactlyInAnyOrder("CREATED", "SMALL_GROUP_OPEN_VOTE_LIMIT_EXCEEDED");
                try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM `" + schema + "`.small_group_vote")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong(1)).isEqualTo(5L);
                }
                try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM `" + schema + "`.small_group_vote_option")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong(1)).isEqualTo(2L);
                }
            } finally {
                workers.shutdownNow();
                assertThat(workers.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
                statement.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }

    // AI작성: 연결별 트랜잭션 안에서 기존 Service를 호출하고, 성공은 커밋·한도 오류는 롤백한다.
    private String createVoteInTransaction(String schema, CountDownLatch ready, CountDownLatch start) throws Exception {
        try (Connection connection = DriverManager.getConnection(System.getenv("MOA_VOTE_TEST_JDBC_URL"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_USER", "root"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_PASSWORD", ""))) {
            connection.setCatalog(schema);
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("SET SESSION innodb_lock_wait_timeout = 10");
            }
            Configuration configuration = new Configuration();
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setEnvironment(new Environment("vote-test", new JdbcTransactionFactory(),
                    new UnpooledDataSource()));
            try (InputStream input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
                new XMLMapperBuilder(input, configuration, "mappers/SmallGroupMapper.xml",
                        configuration.getSqlFragments()).parse();
            }
            try (var session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection)) {
                SmallGroupServiceImpl service = new SmallGroupServiceImpl(session.getMapper(SmallGroupMapper.class));
                SmallGroupVoteCreateRequest request = new SmallGroupVoteCreateRequest();
                request.setTitle("동시 생성 검증");
                request.setOptions(List.of("오전 모임", "오후 모임"));
                request.setMultipleChoice(false);
                request.setAnonymous(false);
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    service.createSmallGroupVote(10L, 40L, request);
                    session.commit();
                    return "CREATED";
                } catch (ApiException exception) {
                    session.rollback();
                    return exception.getCode();
                }
            }
        }
    }

    // AI작성: 명시한 테스트 전용 MySQL에서만 실행하며 세션 임시 테이블로 서비스 데이터를 변경하지 않는다.
    @Test
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_JDBC_URL", matches = ".+")
    void voteListAppliesVisibilityBoundaryAndEffectiveStatusInMysql() throws Exception {
        try (Connection connection = openVoteTestConnection()) {
            insertVote(connection, 1, 10, "OPEN", null, null, false);
            insertVote(connection, 2, 10, "OPEN", "2026-11-03T16:00:00", null, false);
            insertVote(connection, 3, 10, "OPEN", "2026-11-02T15:00:01", null, false);
            insertVote(connection, 4, 10, "OPEN", "2026-11-02T15:00:00", null, false);
            insertVote(connection, 5, 10, "OPEN", "2026-11-02T14:59:59", null, false);
            insertVote(connection, 6, 10, "CLOSED", "2026-11-01T15:00:00", "2026-11-02T15:00:00", false);
            insertVote(connection, 7, 10, "CLOSED", "2026-11-03T16:00:00", "2026-11-02T14:59:59", false);
            insertVote(connection, 8, 10, "CLOSED", null, null, false);
            insertVote(connection, 9, 10, "OPEN", null, null, true);
            insertVote(connection, 10, 99, "OPEN", null, null, false);
            insertVote(connection, 11, 10, "CLOSED", null, "2026-11-02T15:00:01", false);
            insertVote(connection, 12, 10, "OPEN", "2026-11-03T15:00:00", null, false);

            List<Long> ids = new ArrayList<>();
            List<String> statuses = new ArrayList<>();
            try (PreparedStatement query = voteQuery(connection, "selectSmallGroupVoteList", 10);
                    ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    ids.add(rows.getLong("small_group_vote_id"));
                    statuses.add(rows.getString("status"));
                }
            }
            assertThat(ids).containsExactly(12L, 11L, 6L, 4L, 3L, 2L, 1L);
            assertThat(statuses).containsExactly("CLOSED", "CLOSED", "CLOSED", "CLOSED", "CLOSED", "OPEN", "OPEN");
        }
    }

    // AI작성: 만료·명시 종료·삭제·다른 모임 투표를 실제 SQL 집계에서 제외하는지 검증한다.
    @Test
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_JDBC_URL", matches = ".+")
    void openVoteCountExcludesClosedExpiredDeletedAndOtherGroupsInMysql() throws Exception {
        try (Connection connection = openVoteTestConnection()) {
            insertVote(connection, 1, 10, "OPEN", null, null, false);
            insertVote(connection, 2, 10, "OPEN", "2026-11-03T15:00:01", null, false);
            insertVote(connection, 3, 10, "OPEN", "2026-11-03T15:00:00", null, false);
            insertVote(connection, 4, 10, "OPEN", "2026-11-03T14:59:59", null, false);
            insertVote(connection, 5, 10, "CLOSED", null, null, false);
            insertVote(connection, 6, 10, "OPEN", null, "2026-11-03T14:00:00", false);
            insertVote(connection, 7, 10, "OPEN", null, null, true);
            insertVote(connection, 8, 99, "OPEN", null, null, false);
            assertThat(openVoteCount(connection, 10)).isEqualTo(2L);
            assertThat(openVoteCount(connection, 99)).isEqualTo(1L);
            assertThat(openVoteCount(connection, 100)).isZero();
            insertVote(connection, 9, 10, "OPEN", null, null, false);
            insertVote(connection, 10, 10, "OPEN", null, null, false);
            assertThat(openVoteCount(connection, 10)).isEqualTo(4L);
            insertVote(connection, 11, 10, "OPEN", null, null, false);
            assertThat(openVoteCount(connection, 10)).isEqualTo(5L);
        }
    }

    // AI작성: 테스트 전용 접속 정보만 환경 변수로 받고 MySQL 세션 시각을 고정해 경계 검증을 재현한다.
    private Connection openVoteTestConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(System.getenv("MOA_VOTE_TEST_JDBC_URL"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_USER", "root"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_PASSWORD", ""));
        try (var statement = connection.createStatement()) {
            statement.execute("SET SESSION time_zone = '+09:00'");
            statement.execute("SET timestamp = UNIX_TIMESTAMP('2026-11-03 15:00:00')");
            statement.execute("""
                    CREATE TEMPORARY TABLE small_group_vote (
                        small_group_vote_id BIGINT PRIMARY KEY, small_group_id BIGINT NOT NULL,
                        title VARCHAR(100), status VARCHAR(20), ends_at DATETIME, closed_at DATETIME,
                        deleted_at DATETIME, created_at DATETIME NOT NULL
                    ) ENGINE=InnoDB
                    """);
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    // AI작성: 실제 Mapper가 생성한 SQL을 실행하므로 테스트에 조회 조건을 별도로 복제하지 않는다.
    private PreparedStatement voteQuery(Connection connection, String statementId, long groupId) throws Exception {
        return voteQuery(connection, statementId, Map.of("smallGroupId", groupId));
    }

    // AI작성: 조회·종료 SQL 모두 실제 MyBatis 파라미터 순서에 따라 ID를 바인딩한다.
    private PreparedStatement voteQuery(Connection connection, String statementId, Map<String, Long> parameters)
            throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mappers/SmallGroupMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        var boundSql = configuration.getMappedStatement(NAMESPACE + statementId).getBoundSql(parameters);
        PreparedStatement query = connection.prepareStatement(boundSql.getSql());
        for (int index = 0; index < boundSql.getParameterMappings().size(); index++) {
            query.setLong(index + 1, parameters.get(boundSql.getParameterMappings().get(index).getProperty()));
        }
        return query;
    }

    private long openVoteCount(Connection connection, long groupId) throws Exception {
        try (PreparedStatement query = voteQuery(connection, "selectOpenSmallGroupVoteCountForUpdate", groupId);
                ResultSet rows = query.executeQuery()) {
            assertThat(rows.next()).isTrue();
            return rows.getLong(1);
        }
    }

    // AI작성: 고정된 가상 투표만 연결별 임시 테이블에 넣고 연결 종료 시 모두 제거한다.
    private void insertVote(Connection connection, long id, long groupId, String status,
            String endsAt, String closedAt, boolean deleted) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO small_group_vote VALUES (?, ?, '검증용 투표', ?, ?, ?, ?, '2026-11-01 00:00:00')
                """)) {
            insert.setLong(1, id);
            insert.setLong(2, groupId);
            insert.setString(3, status);
            insert.setTimestamp(4, endsAt == null ? null : Timestamp.valueOf(LocalDateTime.parse(endsAt)));
            insert.setTimestamp(5, closedAt == null ? null : Timestamp.valueOf(LocalDateTime.parse(closedAt)));
            insert.setTimestamp(6, deleted ? Timestamp.valueOf("2026-11-03 14:00:00") : null);
            insert.executeUpdate();
        }
    }

    @Test
    void parsesSmallGroupMapperStatements() {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
            assertThat(input).isNotNull();

            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    input, configuration, "mappers/SmallGroupMapper.xml", configuration.getSqlFragments());
            mapperBuilder.parse();
        } catch (Exception exception) {
            throw new AssertionError("소모임 Mapper XML을 파싱할 수 없습니다.", exception);
        }

        // AI작성: 후기의 조회·중복 확인·저장 계약과 생성 키 경로가 MyBatis에서 파싱되는지 확인한다.
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupReviewCount")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupReviewList")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupReviewCreateTargetForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupReviewId")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "insertSmallGroupReview")).isTrue();
        assertThat(configuration.getMappedStatement(NAMESPACE + "insertSmallGroupReview").getKeyProperties())
                .containsExactly("command.smallGroupReviewId");
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupSettingsAuthority")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupCurrentSettings")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupSettingsAuthorityForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupOwnerMemberIdForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectApprovedSmallGroupMemberCountForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "updateSmallGroupBasicSettings")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "updateSmallGroupDetailSettings")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupJoinMemberForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupJoinTargetForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupMembershipForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "insertSmallGroupJoinRequest")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "reactivateLeftSmallGroupMembership")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteListTarget")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteList")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteDetail")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteDetailForUpdate")).isTrue();
        // AI작성: 기존 투표 조회·삭제 계약과 함께 종료 갱신 계약을 파싱한다.
        assertThat(configuration.hasStatement(NAMESPACE + "closeSmallGroupVote")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "softDeleteSmallGroupVote")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteOptionIds")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteDetailOptions")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteParticipantCount")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteParticipationId")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteParticipationIdForUpdate")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "insertSmallGroupVoteParticipation")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "insertSmallGroupVoteSelections")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "deleteSmallGroupVoteSelections")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "updateSmallGroupVoteParticipationTime")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteSelectedOptionIds")).isTrue();
        assertThat(configuration.hasStatement(NAMESPACE + "selectSmallGroupVoteOptionVoterNicknames")).isTrue();
        assertThat(configuration.getResultMapNames()).contains(NAMESPACE + "smallGroupSettings");
    }
}
