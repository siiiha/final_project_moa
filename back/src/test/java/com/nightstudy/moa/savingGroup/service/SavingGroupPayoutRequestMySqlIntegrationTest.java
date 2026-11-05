package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Locale;
import java.util.ArrayList;
import org.junit.jupiter.api.parallel.ResourceLock;
import java.util.Objects;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// AI작성: 명시적으로 허용한 비어 있는 전용 테스트 DB에서만 테이블을 만들고 생성한 테이블만 정리한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@ResourceLock("moa-join-test-database")
class SavingGroupPayoutRequestMySqlIntegrationTest {
    @Test void actualMapperFiltersMembershipAndKeepsReviewerHistory() throws Exception {
        String url = Objects.requireNonNull(System.getenv("MOA_TEST_DB_URL"));
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        try (Connection connection = DriverManager.getConnection(url,
                Objects.requireNonNull(System.getenv("MOA_TEST_DB_USER")),
                Objects.requireNonNull(System.getenv("MOA_TEST_DB_PASSWORD")))) {
            // AI작성: 기존 테스트와 같은 전용 빈 DB 조건을 강제하고 일반 프로젝트 DB에서는 실행하지 않는다.
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var existing = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(existing.next()).as("테스트 DB는 비어 있어야 합니다.").isFalse();
            }
            var created = new ArrayList<String>();
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE members (member_id BIGINT PRIMARY KEY, name VARCHAR(50), deleted_at DATETIME)");
                    created.add("members");
                    statement.execute("CREATE TABLE group_member (group_id BIGINT, member_id BIGINT, membership_status VARCHAR(20))");
                    created.add("group_member");
                    statement.execute("CREATE TABLE meeting_round (round_id BIGINT PRIMARY KEY, group_id BIGINT, round_number INT)");
                    created.add("meeting_round");
                    statement.execute("CREATE TABLE payout_request (payout_request_id BIGINT PRIMARY KEY, round_id BIGINT, requester_id BIGINT, reviewer_id BIGINT, amount DECIMAL(15,2), status VARCHAR(20), requested_at DATETIME, reviewed_at DATETIME)");
                    created.add("payout_request");
                    statement.execute("CREATE TABLE expenses (expense_id BIGINT PRIMARY KEY, group_id BIGINT, applicant_id BIGINT, amount DECIMAL(15,2), title VARCHAR(100), receipt_img_url VARCHAR(500), status VARCHAR(20), created_at DATETIME)");
                    created.add("expenses");
                    statement.execute("INSERT INTO members VALUES (1,'테스트요청자',NULL),(2,'탈퇴요청자',NULL),(3,'삭제요청자',NOW()),(4,'탈퇴처리자',NULL),(5,'타모임회원',NULL)");
                    statement.execute("INSERT INTO group_member VALUES (20,1,'APPROVED'),(20,2,'LEFT'),(20,3,'APPROVED'),(20,4,'LEFT'),(21,5,'APPROVED')");
                    statement.execute("INSERT INTO meeting_round VALUES (100,20,3),(101,21,1)");
                    for (int i = 1; i <= 11; i++) {
                        statement.execute("INSERT INTO payout_request VALUES (" + i + ",100,1,4,1234.50,'APPROVED','2026-11-05 10:00:00','2026-11-05 11:00:00')");
                    }
                    statement.execute("INSERT INTO payout_request VALUES (12,100,2,NULL,1,'PENDING',NOW(),NULL),(13,100,3,NULL,1,'PENDING',NOW(),NULL),(14,101,5,NULL,1,'PENDING',NOW(),NULL)");
                    statement.execute("INSERT INTO expenses VALUES (1,20,1,1000,'테스트 다과','mock-receipt','PENDING',NOW()),(2,20,2,1000,'탈퇴 요청',NULL,'PENDING',NOW()),(3,21,5,1000,'다른 모임',NULL,'PENDING',NOW()),(4,20,1,1000,'증빙 없음','','REJECTED',NOW())");
                }
                Configuration config = new Configuration();
                // AI작성: 세션 종료 후에도 정리용 원본 연결을 유지한다.
                config.setEnvironment(new Environment("payout-test", new JdbcTransactionFactory(), new SingleConnectionDataSource(connection, true)));
                config.setMapUnderscoreToCamelCase(true);
                config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
                try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
                    new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
                }
                try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
                    var mapper = session.getMapper(SavingGroupMapper.class);
                    assertThat(mapper.selectSavingGroupPayoutRequestCount(20L, "순번계", null)).isEqualTo(11);
                    assertThat(mapper.selectSavingGroupPayoutRequestCount(20L, "순번계", "PENDING")).isZero();
                    var first = mapper.selectSavingGroupPayoutRequests(20L, "순번계", "APPROVED", 0);
                    var second = mapper.selectSavingGroupPayoutRequests(20L, "순번계", "APPROVED", 10);
                    assertThat(first).hasSize(10); assertThat(second).hasSize(1);
                    assertThat(first.get(0).getRequestId()).isEqualTo("11");
                    assertThat(second.get(0).getRequestId()).isEqualTo("1");
                    assertThat(first.get(0).getRequesterName()).isEqualTo("테스트요청자");
                    assertThat(first.get(0).getReviewerName()).isNull();
                    assertThat(first.get(0).getReviewedAt()).isNotNull();
                    assertThat(first.get(0).getRoundNumber()).isEqualTo(3);
                    assertThat(first.get(0).getAmount()).isEqualByComparingTo("1234.50");
                    assertThat(mapper.selectSavingGroupPayoutRequestCount(20L, "친목계", null)).isEqualTo(2);
                    var expenses = mapper.selectSavingGroupPayoutRequests(20L, "친목계", "PENDING", 0);
                    assertThat(expenses).hasSize(1);
                    assertThat(expenses.get(0).getTitle()).isEqualTo("테스트 다과");
                    assertThat(expenses.get(0).getHasEvidence()).isTrue();
                    assertThat(expenses.get(0).getRoundNumber()).isNull();
                    assertThat(mapper.selectSavingGroupPayoutRequests(20L, "친목계", "REJECTED", 0).get(0).getHasEvidence()).isFalse();
                    try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT COUNT(*) FROM payout_request")) {
                        result.next(); assertThat(result.getInt(1)).isEqualTo(14);
                    }
                }
            } finally {
                // AI작성: 생성 성공을 기록한 테스트 테이블만 역순으로 정리한다.
                try (var cleanup = connection.createStatement()) {
                    for (int i = created.size() - 1; i >= 0; i--) cleanup.execute("DROP TABLE " + created.get(i));
                }
            }
        }
    }
}
