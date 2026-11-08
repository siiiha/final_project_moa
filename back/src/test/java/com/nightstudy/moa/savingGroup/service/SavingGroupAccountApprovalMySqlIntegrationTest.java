package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.notification.service.NotificationService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mybatis.spring.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 명시적으로 활성화한 빈 테스트 DB에서만 계좌·이력의 실제 커밋과 롤백을 검증한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceLock("moa-join-test-database")
class SavingGroupAccountApprovalMySqlIntegrationTest {
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    VirtualAccountStorageMapper accounts;
    VirtualAccountIssueStateService state;
    final List<String> created = new ArrayList<>();
    @BeforeAll void setup() throws Exception {
        String url = Objects.requireNonNull(System.getenv("MOA_TEST_DB_URL"));
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        var ds = new DriverManagerDataSource(); ds.setDriverClassName("com.mysql.cj.jdbc.Driver"); ds.setUrl(url);
        ds.setUsername(Objects.requireNonNull(System.getenv("MOA_TEST_DB_USER")));
        ds.setPassword(Objects.requireNonNull(System.getenv("MOA_TEST_DB_PASSWORD")));
        var limits = new Properties(); limits.setProperty("connectTimeout", "5000"); limits.setProperty("socketTimeout", "15000"); ds.setConnectionProperties(limits);
        try (var connection = ds.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(tables.next()).as("빈 테스트 전용 DB만 허용").isFalse();
            }
        }
        jdbc = new JdbcTemplate(ds); jdbc.setQueryTimeout(10); manager = new DataSourceTransactionManager(ds); manager.setDefaultTimeout(20);
        try {
            String schema = new ClassPathResource("schema.sql").getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            var ddl = java.util.regex.Pattern.compile("(?s)CREATE TABLE IF NOT EXISTS `group_account`\\s*\\(.*?\\)\\s*ENGINE=[^;]+;").matcher(schema);
            assertThat(ddl.find()).isTrue(); jdbc.execute(ddl.group()); created.add("group_account");
            jdbc.execute("ALTER TABLE group_account ADD CONSTRAINT uq_group_account_group UNIQUE(group_id)");
            // AI작성: 전달된 변경안의 테스트 전용 복제이며 공용·운영 DB에는 적용하지 않는다.
            jdbc.execute("""
                CREATE TABLE saving_group_account_issue (
                  issue_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                  approval_id BIGINT NOT NULL UNIQUE, group_id BIGINT NOT NULL UNIQUE,
                  order_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
                  status ENUM('REQUESTED','COMPLETED','FAILED','UNKNOWN') NOT NULL,
                  account_id BIGINT NULL UNIQUE, error_code VARCHAR(100) NULL,
                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """); created.add("saving_group_account_issue");
            var factory = new SqlSessionFactoryBean(); factory.setDataSource(ds);
            var config = new org.apache.ibatis.session.Configuration(); config.setMapUnderscoreToCamelCase(true); config.setDefaultStatementTimeout(10);
            factory.setConfiguration(config); factory.setMapperLocations(new ClassPathResource("mappers/VirtualAccountStorageMapper.xml"));
            accounts = new SqlSessionTemplate(factory.getObject()).getMapper(VirtualAccountStorageMapper.class);
            state = proxy(new VirtualAccountIssueStateServiceImpl(accounts), VirtualAccountIssueStateService.class);
        } catch (Exception e) { cleanup(); throw e; }
    }
    <T> T proxy(T target, Class<T> contract) {
        var factory = new ProxyFactory(target); factory.setInterfaces(contract);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return contract.cast(factory.getProxy());
    }
    @AfterAll void cleanup() {
        if (jdbc != null) for (int i = created.size() - 1; i >= 0; i--) jdbc.execute("DROP TABLE `" + created.get(i) + "`");
        created.clear();
    }
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM saving_group_account_issue"); jdbc.update("DELETE FROM group_account");
    }
    SavingGroupCreationService creation(VirtualAccountIssueService issuer, boolean failApproval) {
        var storage = proxy(new VirtualAccountStorageServiceImpl(accounts, AccountNumberCipherTest.cipher()), VirtualAccountStorageService.class);
        var approval = proxy(new VirtualAccountApprovalServiceImpl(issuer, storage, state), VirtualAccountApprovalService.class);
        var mapper = mock(SavingGroupMapper.class);
        var application = new SavingGroupCreationApplicationDetailResponse(); application.setApprovalId(1L);
        application.setGroupId(2L); application.setType("CREATE"); application.setStatus("PENDING");
        when(mapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(mapper.approveSavingGroupCreationApplication(1L)).thenReturn(failApproval ? 0 : 1);
        if (!failApproval) { when(mapper.publishApprovedSavingGroup(2L)).thenReturn(1); when(mapper.approveSavingGroupCreationOwnerMembership(2L, 3L)).thenReturn(1); }
        return proxy(new SavingGroupCreationServiceImpl(mapper, mock(FileStorageService.class), approval, mock(NotificationService.class)), SavingGroupCreationService.class);
    }
    @Test void storesAccountAndCompletesHistory() {
        creation(new MockVirtualAccountIssueServiceImpl(), false).approveCreationApplication(1L, 3L);
        assertThat(accounts.selectIssue(1L).getStatus()).isEqualTo("COMPLETED");
        assertThat(accounts.selectAccount(2L).getAccountNumber()).startsWith("v1:").doesNotContain("X000000000000");
    }
    @Test void laterApprovalFailureRollsBackAccountButRetainsUnknown() {
        assertThatThrownBy(() -> creation(new MockVirtualAccountIssueServiceImpl(), true).approveCreationApplication(1L, 3L)).isInstanceOf(ApiException.class);
        assertThat(accounts.selectAccount(2L)).isNull(); assertThat(accounts.selectIssue(1L).getStatus()).isEqualTo("UNKNOWN");
        assertThatThrownBy(() -> state.begin(1L, 2L)).isInstanceOf(ApiException.class);
    }
    @Test void timeoutRetainsUnknownAndPreventsReissue() {
        var issuer = mock(VirtualAccountIssueService.class);
        when(issuer.issue(any())).thenThrow(new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "ACCOUNT_ISSUE_UNKNOWN", "확인 필요"));
        var creation = creation(issuer, false);
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 3L)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 3L)).isInstanceOf(ApiException.class);
        verify(issuer, times(1)).issue(any()); assertThat(accounts.selectIssue(1L).getStatus()).isEqualTo("UNKNOWN");
    }
    @Test @Timeout(30) void concurrentReservationsHaveOnlyOneWinner() throws Exception {
        var workers = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) futures.add(workers.submit(() -> {
                gate.await(); try { state.begin(1L, 2L); return true; } catch (ApiException e) { return false; }
            }));
            gate.countDown(); int winners = 0; for (var result : futures) if (result.get(20, TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
        } finally { workers.shutdownNow(); }
    }
}
