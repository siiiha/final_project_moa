package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

// AI작성: 기존 방식대로 명시적으로 허용한 빈 전용 DB에서만 실제 MySQL 조회를 검증한다.
@EnabledIfEnvironmentVariable(named="MOA_TEST_DB_ENABLED", matches="true")
@ResourceLock("moa-join-test-database")
class SavingGroupPaymentStatusMySqlIntegrationTest {
    @Test void isolatesGroupAndMonthAndKeepsMembersWithoutDeposits() throws Exception {
        String url = Objects.requireNonNull(System.getenv("MOA_TEST_DB_URL"));
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        try (var connection = DriverManager.getConnection(url, Objects.requireNonNull(System.getenv("MOA_TEST_DB_USER")),
                Objects.requireNonNull(System.getenv("MOA_TEST_DB_PASSWORD")))) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var existing = connection.getMetaData().getTables(connection.getCatalog(),null,"%",new String[]{"TABLE","VIEW"})) {
                assertThat(existing.next()).as("전용 테스트 DB는 비어 있어야 합니다.").isFalse();
            }
            // AI작성: 연결 범위 임시 테이블만 사용하므로 실제 거래나 스키마를 변경하지 않는다.
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMPORARY TABLE members (member_id BIGINT PRIMARY KEY, name VARCHAR(30), deleted_at DATETIME)");
                statement.execute("CREATE TEMPORARY TABLE group_member (crew_member_id BIGINT PRIMARY KEY, group_id BIGINT, member_id BIGINT, membership_status VARCHAR(20))");
                statement.execute("CREATE TEMPORARY TABLE group_account (account_id BIGINT PRIMARY KEY, group_id BIGINT)");
                statement.execute("CREATE TEMPORARY TABLE group_transaction (transaction_id BIGINT PRIMARY KEY, account_id BIGINT, amount DECIMAL(15,2), transaction_type VARCHAR(20), transacted_at DATETIME)");
                statement.execute("CREATE TEMPORARY TABLE saving_group_mock_deposit_request (transaction_id BIGINT UNIQUE, account_id BIGINT, member_id BIGINT, status VARCHAR(30))");
                statement.execute("CREATE TEMPORARY TABLE saving_group (group_id BIGINT PRIMARY KEY, dues_amount DECIMAL(12,2), start_date DATE)");
                statement.execute("CREATE TEMPORARY TABLE saving_group_setting_change (setting_change_id BIGINT PRIMARY KEY, group_id BIGINT, dues_amount DECIMAL(12,2), status VARCHAR(20), effective_at DATETIME)");
                statement.execute("INSERT INTO members VALUES (1,'A',NULL),(2,'A',NULL),(3,'C',NULL)");
                statement.execute("INSERT INTO group_member VALUES (1,2,1,'APPROVED'),(2,2,2,'APPROVED'),(3,2,3,'LEFT')");
                statement.execute("INSERT INTO group_account VALUES (11,2),(12,3)");
                statement.execute("""
                        INSERT INTO group_transaction VALUES
                        (1,11,2000,'DEPOSIT','2026-11-01'),(2,11,10000,'DEPOSIT','2026-11-30 23:59:59'),
                        (3,11,12000,'DEPOSIT','2026-12-01'),(4,12,12000,'DEPOSIT','2026-11-01'),
                        (5,11,12000,'DEPOSIT','2026-11-01'),(6,11,12000,'DEPOSIT','2026-11-01')
                        """);
                statement.execute("""
                        INSERT INTO saving_group_mock_deposit_request VALUES
                        (1,11,1,'COMPLETED'),(2,11,1,'COMPLETED'),(3,11,1,'COMPLETED'),
                        (4,12,2,'COMPLETED'),(5,11,2,'PENDING_CONFIRMATION')
                        """);
                statement.execute("INSERT INTO saving_group VALUES (2,12000,'2026-10-01')");
            }
            var config = new Configuration();
            config.setEnvironment(new Environment("transaction-test",new JdbcTransactionFactory(),new SingleConnectionDataSource(connection,true)));
            config.setMapUnderscoreToCamelCase(true);
            config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
            try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
                new XMLMapperBuilder(reader,config,"mappers/SavingGroupMapper.xml",config.getSqlFragments()).parse();
            }
            try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
                var mapper = session.getMapper(SavingGroupMapper.class);
                var start = LocalDateTime.of(2026, 11, 1, 0, 0);
                var end = start.plusMonths(1);
                assertThat(mapper.countSavingGroupPaymentStatus(2L)).isEqualTo(2);
                assertThat(mapper.selectMonthlyPaymentDues(2L, end)).isEqualByComparingTo("12000");
                assertThat(mapper.selectSavingGroupPaymentStatus(2L, start, end, 0, 1))
                        .singleElement().satisfies(row -> assertThat(row.depositedAmount()).isEqualByComparingTo("12000"));
                assertThat(mapper.selectSavingGroupPaymentStatus(2L, start, end, 1, 1))
                        .singleElement().satisfies(row -> assertThat(row.depositedAmount()).isEqualByComparingTo("0"));

                try (var statement = connection.createStatement()) {
                    statement.execute("INSERT INTO saving_group_setting_change VALUES (1,2,15000,'APPLIED','2026-12-01'),(2,2,18000,'PENDING','2027-01-01')");
                }
                session.clearCache();
                assertThat(mapper.selectMonthlyPaymentDues(2L, end)).isNull();
                assertThat(mapper.selectMonthlyPaymentDues(2L, end.plusMonths(2))).isEqualByComparingTo("15000");
            }
        }
    }
}
