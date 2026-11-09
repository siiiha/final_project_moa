package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.DriverManager;
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
class SavingGroupTransactionMySqlIntegrationTest {
    @Test void isolatesAccountsAndPaginatesStableOrder() throws Exception {
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
                statement.execute("CREATE TEMPORARY TABLE group_transaction (transaction_id BIGINT PRIMARY KEY, account_id BIGINT, transaction_type VARCHAR(20), amount DECIMAL(15,2), balance_after DECIMAL(15,2), transacted_at DATETIME, description VARCHAR(255))");
                for (int id = 1; id <= 12; id++) {
                    statement.execute("INSERT INTO group_transaction VALUES (" + id
                            + ",3,'DEPOSIT'," + id + ",0,'2026-11-08 12:00:00','테스트 입금')");
                }
                statement.execute("INSERT INTO group_transaction VALUES (13,4,'WITHDRAWAL',100,0,'2026-11-08 12:00:00','다른 계좌')");
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
                assertThat(mapper.selectSavingGroupTransactionCount(3L)).isEqualTo(12);
                var first = mapper.selectSavingGroupTransactions(3L, 0L, 10);
                var second = mapper.selectSavingGroupTransactions(3L, 10L, 10);
                assertThat(first).hasSize(10);
                assertThat(second).hasSize(2);
                assertThat(first.get(0).amount()).isEqualByComparingTo("12");
                assertThat(second.get(1).amount()).isEqualByComparingTo("1");
                assertThat(first.get(0).balanceAfter()).isEqualByComparingTo("0");
                assertThat(first.get(0).description()).isEqualTo("테스트 입금");
                assertThat(mapper.selectSavingGroupTransactions(3L, 20L, 10)).isEmpty();
                assertThat(mapper.selectSavingGroupTransactionCount(4L)).isEqualTo(1);
            }
        }
    }
}
