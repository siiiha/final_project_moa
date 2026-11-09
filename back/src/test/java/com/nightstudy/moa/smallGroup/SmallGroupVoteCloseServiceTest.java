package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteParticipationRequest;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

// AI작성: 작성자·개설자·승인 OWNER 권한과 종료 재요청·마감 경계·갱신 실패를 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupVoteCloseServiceTest {
    private static final long GROUP = 20L;
    private static final long SMALL_GROUP = 10L;
    private static final long VOTE = 30L;
    private static final long OWNER = 40L;
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 15, 0);

    @Mock private SmallGroupMapper mapper;
    @InjectMocks private SmallGroupServiceImpl service;

    // AI작성: 테스트 전용 MySQL의 별도 스키마에서 먼저 잠근 트랜잭션이 끝날 때까지 후속 요청이 대기하는지 확인한다.
    // close가 먼저면 참여 거절, 참여가 먼저면 이력 보존 후 종료하며 Spring HTTP·JWT는 이 검증에 포함하지 않는다.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @EnabledIfEnvironmentVariable(named = "MOA_VOTE_TEST_CONCURRENCY", matches = "true")
    void closeAndParticipationSerializeInMysql(boolean closeFirst) throws Exception {
        String schema = "moa_close_service_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection admin = mysqlConnection(); var sql = admin.createStatement()) {
            sql.execute("CREATE DATABASE `" + schema + "`");
            var workers = Executors.newSingleThreadExecutor();
            try {
                for (String ddl : List.of(
                        "CREATE TABLE `groups` (group_id BIGINT PRIMARY KEY, member_id BIGINT, max_members INT, deleted_at DATETIME)",
                        "CREATE TABLE small_group (small_group_id BIGINT PRIMARY KEY, group_id BIGINT)",
                        "CREATE TABLE members (member_id BIGINT PRIMARY KEY, nickname VARCHAR(50), deleted_at DATETIME)",
                        "CREATE TABLE group_member (group_id BIGINT, member_id BIGINT, member_role VARCHAR(20), membership_status VARCHAR(20), PRIMARY KEY(group_id,member_id))",
                        "CREATE TABLE small_group_vote (small_group_vote_id BIGINT PRIMARY KEY, small_group_id BIGINT, creator_member_id BIGINT, title VARCHAR(100), is_multiple_choice BOOLEAN, is_anonymous BOOLEAN, ends_at DATETIME, status VARCHAR(20), closed_at DATETIME, deleted_at DATETIME, created_at DATETIME, updated_at DATETIME)",
                        "CREATE TABLE small_group_vote_option (small_group_vote_option_id BIGINT PRIMARY KEY, small_group_vote_id BIGINT, display_order INT)",
                        "CREATE TABLE small_group_vote_participation (small_group_vote_participation_id BIGINT AUTO_INCREMENT PRIMARY KEY, small_group_vote_id BIGINT, member_id BIGINT, UNIQUE(small_group_vote_id,member_id))",
                        "CREATE TABLE small_group_vote_selection (small_group_vote_participation_id BIGINT, small_group_vote_option_id BIGINT)")) {
                    sql.execute("USE `" + schema + "`");
                    sql.execute(ddl + " ENGINE=InnoDB");
                }
                sql.execute("INSERT INTO `groups` VALUES (20,40,10,NULL)");
                sql.execute("INSERT INTO small_group VALUES (10,20)");
                sql.execute("INSERT INTO members VALUES (40,'검증용 작성자',NULL),(50,'검증용 참여자',NULL)");
                sql.execute("INSERT INTO group_member VALUES (20,40,'OWNER','APPROVED'),(20,50,'MEMBER','APPROVED')");
                sql.execute("INSERT INTO small_group_vote VALUES (30,10,40,'검증용 투표',TRUE,TRUE,NULL,'OPEN',NULL,NULL,NOW(),NOW())");
                sql.execute("INSERT INTO small_group_vote_option VALUES (101,30,1),(102,30,2)");
                SqlSessionFactory factory = mysqlSessionFactory();
                try (Connection firstConnection = mysqlConnection()) {
                    firstConnection.setCatalog(schema);
                    firstConnection.setAutoCommit(false);
                    try (var first = factory.openSession(firstConnection)) {
                        SmallGroupServiceImpl firstService = new SmallGroupServiceImpl(first.getMapper(SmallGroupMapper.class));
                        applyOperation(firstService, closeFirst);
                        CountDownLatch secondStarted = new CountDownLatch(1);
                        var second = workers.submit(() -> {
                            try (Connection secondConnection = mysqlConnection()) {
                                secondConnection.setCatalog(schema);
                                secondConnection.setAutoCommit(false);
                                try (var session = factory.openSession(secondConnection)) {
                                    secondStarted.countDown();
                                    try {
                                        applyOperation(new SmallGroupServiceImpl(session.getMapper(SmallGroupMapper.class)), !closeFirst);
                                        session.commit();
                                        return "SUCCESS";
                                    } catch (ApiException error) {
                                        session.rollback();
                                        return error.getCode();
                                    }
                                }
                            }
                        });
                        assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
                        // AI작성: 첫 트랜잭션의 소모임 행 잠금이 유지되는 동안 후속 요청은 완료될 수 없다.
                        org.junit.jupiter.api.Assertions.assertThrows(TimeoutException.class,
                                () -> second.get(300, TimeUnit.MILLISECONDS));
                        first.commit();
                        assertThat(second.get(15, TimeUnit.SECONDS))
                                .isEqualTo(closeFirst ? "SMALL_GROUP_VOTE_CLOSED" : "SUCCESS");
                    }
                }
                try (var rows = sql.executeQuery("SELECT status, closed_at FROM small_group_vote WHERE small_group_vote_id=30")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getString("status")).isEqualTo("CLOSED");
                    assertThat(rows.getTimestamp("closed_at")).isNotNull();
                }
                for (String table : List.of("small_group_vote_participation", "small_group_vote_selection")) {
                    try (var rows = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getInt(1)).isEqualTo(closeFirst ? 0 : table.endsWith("selection") ? 2 : 1);
                    }
                }
            } finally {
                workers.shutdownNow();
                try {
                    assertThat(workers.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
                } finally {
                    sql.execute("DROP DATABASE `" + schema + "`");
                }
            }
        }
    }

    // AI작성: 실제 참여 경로에 두 보기를 전달해 복수 선택 매핑과 종료 후 참여 차단도 함께 확인한다.
    private void applyOperation(SmallGroupServiceImpl target, boolean close) {
        if (close) target.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        else {
            SmallGroupVoteParticipationRequest request = new SmallGroupVoteParticipationRequest();
            request.setSelectedOptionIds(List.of(101L, 102L));
            target.participateSmallGroupVote(SMALL_GROUP, VOTE, 50L, request);
        }
    }

    private Connection mysqlConnection() throws Exception {
        Connection connection = DriverManager.getConnection(System.getenv("MOA_VOTE_TEST_JDBC_URL"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_USER", "root"),
                System.getenv().getOrDefault("MOA_VOTE_TEST_DB_PASSWORD", ""));
        try (var sql = connection.createStatement()) {
            sql.execute("SET SESSION innodb_lock_wait_timeout = 10");
        }
        return connection;
    }

    private SqlSessionFactory mysqlSessionFactory() throws Exception {
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("close-service-test", new JdbcTransactionFactory(),
                new UnpooledDataSource()));
        try (var input = getClass().getResourceAsStream("/mappers/SmallGroupMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mappers/SmallGroupMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    // AI작성: 마감 미설정과 미래 마감 모두 종료하며 부모 모임 잠금을 가장 먼저 획득한다.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closesOpenVoteWithOrWithoutDeadline(boolean deadline) {
        SmallGroupVoteDetailRow vote = mockAuthorizedVote();
        vote.setEndsAt(deadline ? NOW.plusHours(1) : null);
        when(mapper.closeSmallGroupVote(SMALL_GROUP, VOTE)).thenReturn(1);
        try (var time = fixedTime()) {
            service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        }
        var order = inOrder(mapper);
        order.verify(mapper).selectSmallGroupJoinTargetForUpdate(SMALL_GROUP);
        order.verify(mapper).selectSmallGroupVoteDetailForUpdate(SMALL_GROUP, VOTE);
        order.verify(mapper).selectSmallGroupMembershipForUpdate(GROUP, OWNER);
        order.verify(mapper).closeSmallGroupVote(SMALL_GROUP, VOTE);
        verifyNoMoreInteractions(mapper);
    }

    // AI작성: 마감 정각·경과 요청은 권한 확인 후 무변경 성공으로 종료한다.
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 90000})
    void expiredVoteSucceedsWithoutUpdating(int secondsAgo) {
        mockAuthorizedVote().setEndsAt(NOW.minusSeconds(secondsAgo));
        try (var time = fixedTime()) {
            service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        }
        verify(mapper).selectSmallGroupMembershipForUpdate(GROUP, OWNER);
        verify(mapper, never()).closeSmallGroupVote(anyLong(), anyLong());
    }

    // AI작성: 이미 종료된 요청은 종료 시각의 유무와 관계없이 갱신하지 않는다.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void alreadyClosedVoteSucceedsWithoutChangingClosedAt(boolean hasClosedAt) {
        SmallGroupVoteDetailRow vote = mockAuthorizedVote();
        vote.setStatus("CLOSED");
        LocalDateTime closedAt = hasClosedAt ? NOW.minusDays(2) : null;
        vote.setClosedAt(closedAt);
        service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        assertThat(vote.getClosedAt()).isEqualTo(closedAt);
        verify(mapper, never()).closeSmallGroupVote(anyLong(), anyLong());
    }

    @Test
    void closedAtPresentDoesNotGetOverwrittenEvenIfStatusIsOpen() {
        mockAuthorizedVote().setClosedAt(NOW.minusHours(1));
        service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        verify(mapper, never()).closeSmallGroupVote(anyLong(), anyLong());
    }

    // AI작성: 검증 후 SQL 실행 사이 마감시각이 지나도 기존 마감시각을 유지하며 성공 처리한다.
    @Test
    void deadlinePassingDuringUpdateSucceedsWithoutNewClosedAt() {
        mockAuthorizedVote().setEndsAt(NOW.plusSeconds(1));
        when(mapper.closeSmallGroupVote(SMALL_GROUP, VOTE)).thenReturn(0);
        try (var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            time.when(() -> LocalDateTime.now(KOREA)).thenReturn(NOW, NOW.plusSeconds(1));
            service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER);
        }
        verify(mapper).closeSmallGroupVote(SMALL_GROUP, VOTE);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void unexpectedUpdateCountFails(int updatedCount) {
        mockAuthorizedVote();
        when(mapper.closeSmallGroupVote(SMALL_GROUP, VOTE)).thenReturn(updatedCount);
        ApiException error = catchThrowableOfType(
                () -> service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER), ApiException.class);
        assertThat(error.getCode()).isEqualTo("SMALL_GROUP_VOTE_CLOSE_FAILED");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // AI작성: 작성자 또는 현재 개설자 중 하나라도 다르면 이미 종료된 투표라도 성공으로 우회하지 못한다.
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsNonAuthorOrFormerOwnerBeforeClosedStateCheck(boolean formerOwner) {
        SmallGroupJoinTargetRow target = target();
        SmallGroupVoteDetailRow vote = vote();
        vote.setStatus("CLOSED");
        if (formerOwner) target.setCreatorMemberId(99L);
        else vote.setCreatorMemberId(99L);
        when(mapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP)).thenReturn(target);
        when(mapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP, VOTE)).thenReturn(vote);
        assertForbidden();
        verify(mapper, never()).selectSmallGroupMembershipForUpdate(anyLong(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({"MEMBER,APPROVED", "MANAGER,APPROVED", "OWNER,PENDING", "OWNER,LEFT", "OWNER,EXPELLED", "OWNER,REJECTED"})
    void rejectsInvalidMembershipEvenForClosedVote(String role, String status) {
        when(mapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP)).thenReturn(target());
        SmallGroupVoteDetailRow vote = vote();
        vote.setStatus("CLOSED");
        when(mapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP, VOTE)).thenReturn(vote);
        when(mapper.selectSmallGroupMembershipForUpdate(GROUP, OWNER)).thenReturn(relation(role, status));
        assertForbidden();
    }

    @Test
    void rejectsMissingMembership() {
        when(mapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP)).thenReturn(target());
        when(mapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP, VOTE)).thenReturn(vote());
        assertForbidden();
    }

    @Test
    void rejectsMissingOrDeletedGroupAndVote() {
        ApiException groupError = catchThrowableOfType(
                () -> service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER), ApiException.class);
        assertThat(groupError.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
        when(mapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP)).thenReturn(target());
        ApiException voteError = catchThrowableOfType(
                () -> service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER), ApiException.class);
        assertThat(voteError.getCode()).isEqualTo("SMALL_GROUP_VOTE_NOT_FOUND");
        verify(mapper, never()).closeSmallGroupVote(anyLong(), anyLong());
    }

    @Test
    void rejectsInvalidIdsBeforeDatabaseAccess() {
        for (Long invalid : new Long[] {null, 0L, -1L}) {
            assertThat(catchThrowableOfType(() -> service.closeSmallGroupVote(invalid, VOTE, OWNER),
                    ApiException.class).getCode()).isEqualTo("BAD_REQUEST");
            assertThat(catchThrowableOfType(() -> service.closeSmallGroupVote(SMALL_GROUP, invalid, OWNER),
                    ApiException.class).getCode()).isEqualTo("BAD_REQUEST");
            assertThat(catchThrowableOfType(() -> service.closeSmallGroupVote(SMALL_GROUP, VOTE, invalid),
                    ApiException.class).getCode()).isEqualTo("UNAUTHORIZED");
        }
        verifyNoInteractions(mapper);
    }

    private void assertForbidden() {
        ApiException error = catchThrowableOfType(
                () -> service.closeSmallGroupVote(SMALL_GROUP, VOTE, OWNER), ApiException.class);
        assertThat(error.getCode()).isEqualTo("SMALL_GROUP_VOTE_CLOSE_FORBIDDEN");
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(mapper, never()).closeSmallGroupVote(anyLong(), anyLong());
    }

    private SmallGroupVoteDetailRow mockAuthorizedVote() {
        SmallGroupVoteDetailRow vote = vote();
        when(mapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP)).thenReturn(target());
        when(mapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP, VOTE)).thenReturn(vote);
        when(mapper.selectSmallGroupMembershipForUpdate(GROUP, OWNER)).thenReturn(relation("OWNER", "APPROVED"));
        return vote;
    }

    private SmallGroupJoinTargetRow target() {
        SmallGroupJoinTargetRow target = new SmallGroupJoinTargetRow();
        target.setSmallGroupId(SMALL_GROUP);
        target.setGroupId(GROUP);
        target.setCreatorMemberId(OWNER);
        return target;
    }

    private SmallGroupVoteDetailRow vote() {
        SmallGroupVoteDetailRow vote = new SmallGroupVoteDetailRow();
        vote.setSmallGroupId(SMALL_GROUP);
        vote.setSmallGroupVoteId(VOTE);
        vote.setCreatorMemberId(OWNER);
        vote.setStatus("OPEN");
        return vote;
    }

    private SmallGroupMemberRelationRow relation(String role, String status) {
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        return relation;
    }

    private MockedStatic<LocalDateTime> fixedTime() {
        var time = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS);
        time.when(() -> LocalDateTime.now(KOREA)).thenReturn(NOW);
        return time;
    }
}
