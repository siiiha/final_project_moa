package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.DriverManager;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

// AI작성: 명시적으로 지정한 테스트 DB에서 연결 전용 임시 테이블만 생성한다. 실제 스키마 변경·금융 API 호출은 없다.
@EnabledIfEnvironmentVariable(named = "MOA_PAYOUT_TEST_DB_ENABLED", matches = "true")
class SavingGroupPayoutDecisionMySqlTest {
    @ParameterizedTest @CsvSource({"순번계,APPROVED", "순번계,REJECTED", "친목계,APPROVED", "친목계,REJECTED"})
    void storesReviewAndRollsBackWithoutMovingFunds(String type, String status) throws Exception {
        String url = System.getenv("MOA_TEST_DB_URL");
        assertThat(url).isNotBlank().doesNotContain("@");
        assertThat(url.toLowerCase(java.util.Locale.ROOT)).doesNotContain("password=", "user=", "createdatabaseifnotexist=true");
        try (var connection = DriverManager.getConnection(url, System.getenv("MOA_TEST_DB_USER"), System.getenv("MOA_TEST_DB_PASSWORD"))) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_payout_test_[A-Za-z0-9_]+");
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TEMPORARY TABLE meeting_round (round_id BIGINT PRIMARY KEY, group_id BIGINT, round_number INT) ENGINE=InnoDB");
                sql.execute("CREATE TEMPORARY TABLE payout_request (payout_request_id BIGINT PRIMARY KEY, round_id BIGINT, requester_id BIGINT, amount DECIMAL(15,2), status VARCHAR(20), reviewer_id BIGINT NULL, reviewed_at DATETIME NULL, reject_reason VARCHAR(500) NULL, transaction_id BIGINT NULL, completed_at DATETIME NULL) ENGINE=InnoDB");
                sql.execute("CREATE TEMPORARY TABLE expenses (expense_id BIGINT PRIMARY KEY, group_id BIGINT, applicant_id BIGINT, amount DECIMAL(15,2), receipt_img_url VARCHAR(500), status VARCHAR(20), reviewer_id BIGINT NULL, reviewed_at DATETIME NULL, reject_reason VARCHAR(500) NULL) ENGINE=InnoDB");
                sql.execute("INSERT INTO meeting_round VALUES (1,20,2)");
                sql.execute("INSERT INTO payout_request (payout_request_id,round_id,requester_id,amount,status) VALUES (40,1,50,1000,'PENDING')");
                sql.execute("INSERT INTO expenses (expense_id,group_id,applicant_id,amount,receipt_img_url,status) VALUES (40,20,50,1000,'private-test-receipt','PENDING')");
            }
            var config = new Configuration(new Environment("payout-test", new JdbcTransactionFactory(), new SingleConnectionDataSource(connection, true)));
            config.setMapUnderscoreToCamelCase(true);
            config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
            try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
                new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
            }
            String table = type.equals("순번계") ? "payout_request" : "expenses";
            String reason = status.equals("REJECTED") ? "증빙 확인 필요" : null;
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession(false)) {
                var mapper = session.getMapper(SavingGroupMapper.class);
                assertThat(mapper.selectPayoutDecisionRequestForUpdate(20L, 40L, type)).isNotNull();
                assertThat(mapper.selectPayoutDecisionRequestForUpdate(99L, 40L, type)).isNull();
                assertThat(mapper.updatePayoutDecision(99L, 40L, type, status, 30L, reason)).isZero();
                assertThat(mapper.updatePayoutDecision(20L, 40L, type, status, 30L, reason)).isEqualTo(1);
                try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT * FROM " + table)) {
                    assertThat(row.next()).isTrue(); assertThat(row.getString("status")).isEqualTo(status);
                    assertThat(row.getLong("reviewer_id")).isEqualTo(30L); assertThat(row.getTimestamp("reviewed_at")).isNotNull();
                    assertThat(row.getString("reject_reason")).isEqualTo(reason); assertThat(row.getBigDecimal("amount")).isEqualByComparingTo("1000");
                    if (type.equals("순번계")) { assertThat(row.getObject("transaction_id")).isNull(); assertThat(row.getTimestamp("completed_at")).isNull(); }
                }
                assertThat(mapper.updatePayoutDecision(20L, 40L, type, status, 30L, reason)).isZero();
                session.rollback();
                try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT status,reviewer_id,reviewed_at,reject_reason FROM " + table)) {
                    assertThat(row.next()).isTrue(); assertThat(row.getString("status")).isEqualTo("PENDING");
                    assertThat(row.getObject("reviewer_id")).isNull(); assertThat(row.getTimestamp("reviewed_at")).isNull(); assertThat(row.getString("reject_reason")).isNull();
                }
                // AI작성: 다음 트랜잭션에서 승인·거절 기록이 커밋되어 보존되는지도 확인한다.
                assertThat(mapper.updatePayoutDecision(20L, 40L, type, status, 30L, reason)).isEqualTo(1);
                session.commit();
            }
            try (var sql = connection.createStatement(); var row = sql.executeQuery("SELECT status FROM " + table)) {
                assertThat(row.next()).isTrue(); assertThat(row.getString("status")).isEqualTo(status);
            }
        }
    }
}

