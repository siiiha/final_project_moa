package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
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
    @Test void actualSqlIncludesBoundariesAndPaginatesStableOrder() throws Exception {
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
                statement.execute("CREATE TEMPORARY TABLE group_transaction (transaction_id BIGINT PRIMARY KEY, account_id BIGINT, transaction_type VARCHAR(20), amount DECIMAL(15,2), balance_after DECIMAL(15,2), transacted_at DATETIME)");
                for (int i=1; i<=12; i++) {
                    int amount = i==1 ? 10000 : i==12 ? 40000 : 10000+i;
                    String date = i==1 ? "2026-11-05 10:00:00" : "2026-11-06 10:00:00";
                    statement.execute("INSERT INTO group_transaction VALUES ("+i+",3,'DEPOSIT',"+amount+",NULL,'"+date+"')");
                }
                statement.execute("INSERT INTO group_transaction VALUES (13,3,'DEPOSIT',9999,100,'2026-11-07 10:00:00'),(14,3,'DEPOSIT',40001,100,'2026-11-07 10:00:00'),(15,4,'DEPOSIT',20000,100,'2026-11-07 10:00:00'),(16,3,'WITHDRAWAL',20000,100,'2026-11-07 10:00:00')");
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
                var c = new SavingGroupTransactionSearchCondition();
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isEqualTo(15);
                c.setTransactionType("DEPOSIT"); c.setMinAmount(new BigDecimal("10000")); c.setMaxAmount(new BigDecimal("40000"));
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isEqualTo(12);
                var first = mapper.selectSavingGroupTransactions(3L,c,0);
                var second = mapper.selectSavingGroupTransactions(3L,c,10);
                assertThat(first).hasSize(10); assertThat(second).hasSize(2);
                assertThat(first.get(0).getAmount()).isEqualByComparingTo("40000");
                assertThat(second.get(1).getAmount()).isEqualByComparingTo("10000");
                assertThat(first.get(0).getBalanceAfter()).isNull();
                assertThat(mapper.selectSavingGroupTransactions(3L,c,20)).isEmpty();
                c.setSort("OLDEST");
                var oldest = mapper.selectSavingGroupTransactions(3L,c,0);
                assertThat(oldest.get(0).getAmount()).isEqualByComparingTo("10000");
                assertThat(oldest.get(1).getAmount()).isEqualByComparingTo("10002");
                c.setTransactionType("WITHDRAWAL");
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isEqualTo(1);
                assertThat(mapper.selectSavingGroupTransactions(3L,c,0).get(0).getBalanceAfter()).isEqualByComparingTo("100");
                c.setMinAmount(null); c.setMaxAmount(new BigDecimal("9999"));
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isZero();
                c.setTransactionType("DEPOSIT");
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isEqualTo(1);
                c.setMaxAmount(null); c.setMinAmount(new BigDecimal("40001"));
                assertThat(mapper.selectSavingGroupTransactionCount(3L,c)).isEqualTo(1);
            }
        }
    }
}
