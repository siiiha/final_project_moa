package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 명시적으로 허용한 비어 있는 전용 DB에만 최소 테스트 테이블을 만들고 실제 Mapper·잠금을 검증한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@ResourceLock("moa-join-test-database")
class SavingGroupDepositMySqlIntegrationTest {
    @Test void actualTransactionsSerializeDuplicateRequestsAndRollbackPartialSave() throws Exception {
        String url = Objects.requireNonNull(System.getenv("MOA_TEST_DB_URL"));
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        var source = new DriverManagerDataSource(url, Objects.requireNonNull(System.getenv("MOA_TEST_DB_USER")),
                Objects.requireNonNull(System.getenv("MOA_TEST_DB_PASSWORD")));
        try (var connection = source.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(tables.next()).as("전용 테스트 DB는 비어 있어야 합니다.").isFalse();
            }
        }
        var jdbc = new JdbcTemplate(source);
        var created = new ArrayList<String>();
        // AI작성: 런타임 스키마 변경이 아닌 테스트 전용 최소 구조이며 실제 전체 제약 적용은 DB 담당자가 검증한다.
        Map<String, String> ddl = new LinkedHashMap<>();
        ddl.put("groups", "group_id BIGINT PRIMARY KEY, deleted_at DATETIME NULL");
        ddl.put("saving_group", "group_id BIGINT PRIMARY KEY, saving_group_type VARCHAR(20), status VARCHAR(2), start_date DATE");
        ddl.put("members", "member_id BIGINT PRIMARY KEY, deleted_at DATETIME NULL");
        ddl.put("group_member", "crew_member_id BIGINT PRIMARY KEY, group_id BIGINT, member_id BIGINT, member_role VARCHAR(20), membership_status VARCHAR(20)");
        ddl.put("group_account", "account_id BIGINT PRIMARY KEY, group_id BIGINT UNIQUE");
        ddl.put("group_transaction", "transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT, transaction_type VARCHAR(20), amount DECIMAL(15,2), balance_after DECIMAL(15,2), transacted_at DATETIME, description VARCHAR(255)");
        ddl.put("saving_group_mock_deposit_request", "deposit_request_id BIGINT AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL, member_id BIGINT NOT NULL, amount DECIMAL(15,2) NOT NULL, request_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL, status VARCHAR(30), window_anchor_request_id BIGINT NULL, confirmation_token_hash CHAR(64) NULL, confirmation_expires_at DATETIME(6) NULL, transaction_id BIGINT NULL UNIQUE, requested_at DATETIME(6) NOT NULL, completed_at DATETIME(6) NULL, UNIQUE KEY(member_id,request_key)");
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (var entry : ddl.entrySet()) {
                jdbc.execute("CREATE TABLE `" + entry.getKey() + "` (" + entry.getValue() + ") ENGINE=InnoDB"); created.add(entry.getKey());
            }
            jdbc.execute("INSERT INTO `groups` VALUES (10,NULL)");
            jdbc.execute("INSERT INTO saving_group VALUES (10,'친목계','02',CURRENT_DATE)");
            jdbc.execute("INSERT INTO members VALUES (20,NULL)");
            jdbc.execute("INSERT INTO group_member VALUES (1,10,20,'MEMBER','APPROVED')");
            jdbc.execute("INSERT INTO group_account VALUES (30,10)");
            Configuration config = new Configuration(); config.setMapUnderscoreToCamelCase(true);
            config.setEnvironment(new Environment("deposit-test", new SpringManagedTransactionFactory(), source));
            config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
            try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
                new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
            }
            var mapper = new SqlSessionTemplate(new SqlSessionFactoryBuilder().build(config)).getMapper(SavingGroupMapper.class);
            var manager = new DataSourceTransactionManager(source);
            var proxy = new ProxyFactory(new SavingGroupDepositServiceImpl(mapper, () -> {}));
            proxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
            var service = (SavingGroupDepositService) proxy.getProxy();
            var gate = new CountDownLatch(1);
            var futures = new ArrayList<Future<SavingGroupDepositResponse>>();
            for (int i = 0; i < 2; i++) {
                String key = "mock-concurrent-key-" + i;
                futures.add(executor.submit(() -> { gate.await(); return service.deposit(10L, 20L, new SavingGroupDepositRequest(new BigDecimal("1000"), key)); }));
            }
            gate.countDown();
            var responses = List.of(futures.get(0).get(15, TimeUnit.SECONDS), futures.get(1).get(15, TimeUnit.SECONDS));
            assertThat(responses).extracting(SavingGroupDepositResponse::status).containsExactlyInAnyOrder("COMPLETED", "PENDING_CONFIRMATION");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_transaction", Integer.class)).isEqualTo(1);
            int pendingIndex = responses.get(0).status().equals("PENDING_CONFIRMATION") ? 0 : 1;
            var confirm = new SavingGroupDepositConfirmRequest("mock-concurrent-key-" + pendingIndex, responses.get(pendingIndex).confirmationToken());
            service.confirm(10L, 20L, confirm); service.confirm(10L, 20L, confirm);
            assertThat(mapper.selectSavingGroupBalance(10L)).isEqualByComparingTo("2000");
            assertThat(jdbc.queryForList("SELECT balance_after FROM group_transaction ORDER BY transaction_id", BigDecimal.class))
                    .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .containsExactly(new BigDecimal("1000"), new BigDecimal("2000"));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_transaction", Integer.class)).isEqualTo(2);
            // AI작성: 원장 INSERT 뒤 보조 기록 실패를 주입해 실제 DB에 부분 거래가 남지 않는지 검증한다.
            var failing = org.mockito.Mockito.mock(SavingGroupMapper.class, org.mockito.AdditionalAnswers.delegatesTo(mapper));
            org.mockito.Mockito.doReturn(0).when(failing).insertDepositRequest(org.mockito.ArgumentMatchers.any());
            var failingProxy = new ProxyFactory(new SavingGroupDepositServiceImpl(failing, () -> {}));
            failingProxy.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
            assertThatThrownBy(() -> ((SavingGroupDepositService) failingProxy.getProxy()).deposit(10L, 20L,
                    new SavingGroupDepositRequest(new BigDecimal("2000"), "mock-rollback-key-0001"))).isInstanceOf(RuntimeException.class);
            assertThat(mapper.selectSavingGroupBalance(10L)).isEqualByComparingTo("2000");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM group_transaction", Integer.class)).isEqualTo(2);
        } finally {
            executor.shutdownNow(); executor.awaitTermination(20, TimeUnit.SECONDS);
            Collections.reverse(created);
            // AI작성: 빈 전용 DB에서 이 테스트가 생성한 고정 이름의 테이블만 정리한다.
            for (String table : created) jdbc.execute("DROP TABLE `" + table + "`");
        }
    }
}
