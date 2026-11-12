package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.CreateRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.Insert;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.http.HttpStatus;

// AI작성: 비어 있는 전용 MySQL DB의 임시 테이블로 생성 키, 월 경계, 모임 분리를 검증한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_DB_ENABLED", matches = "true")
@ResourceLock("moa-join-test-database")
class SavingGroupCalendarMySqlIntegrationTest {
    @Test void savesAndReadsOnlyEventsForRequestedGroupAndMonth() throws Exception {
        String url = System.getenv("MOA_TEST_DB_URL");
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(java.util.Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        try (var connection = DriverManager.getConnection(url, System.getenv("MOA_TEST_DB_USER"),
                System.getenv("MOA_TEST_DB_PASSWORD"))) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(tables.next()).isFalse();
            }
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TEMPORARY TABLE group_event (
                          schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY, group_id BIGINT NOT NULL,
                          writer_id BIGINT NOT NULL, title VARCHAR(100) NOT NULL,
                          location VARCHAR(200), event_date DATETIME NOT NULL
                        )
                        """);
            }
            var factory = mapperFactory(new SingleConnectionDataSource(connection, true));
            var start = LocalDateTime.of(2026, 11, 1, 0, 0);
            var end = start.plusMonths(1);
            var event = new Insert(10L, 30L, "정기 모임", start);
            try (var session = factory.openSession()) {
                var mapper = session.getMapper(SavingGroupMapper.class);
                assertThat(mapper.insertCalendarEvent(event)).isEqualTo(1);
                assertThat(event.scheduleId).isPositive();
                mapper.insertCalendarEvent(new Insert(11L, 30L, "다른 계모임", start));
                mapper.insertCalendarEvent(new Insert(10L, 30L, "다음 달", end));
                session.commit();
            }
            try (var session = factory.openSession()) {
                var mapper = session.getMapper(SavingGroupMapper.class);
                assertThat(mapper.countCalendarEvents(10L, start, start.plusDays(1))).isEqualTo(1);
                assertThat(mapper.countCalendarEvents(11L, start, start.plusDays(1))).isEqualTo(1);
                assertThat(mapper.countCalendarEvents(10L, start.plusDays(1), start.plusDays(2))).isZero();
                // AI작성: 타 모임 ID로 삭제를 시도해도 일정 행은 남는다.
                assertThat(mapper.deleteCalendarEvent(11L, event.scheduleId)).isZero();
                assertThat(mapper.selectCalendarEvents(10L, start, end))
                        .singleElement().satisfies(row -> {
                            assertThat(row.scheduleId()).isEqualTo(event.scheduleId);
                            assertThat(row.title()).isEqualTo("정기 모임");
                            assertThat(row.eventDate()).isEqualTo(start);
                        });
            }
        }
    }

    // AI작성: 두 연결이 동일한 InnoDB 모임 행을 잠그고 마지막 한 자리를 경쟁한다.
    @Test void concurrentOwnerAndManagerCannotBothTakeTheLastSlot() throws Exception {
        String url = System.getenv("MOA_TEST_DB_URL");
        assertThat(url).startsWith("jdbc:mysql://").doesNotContain("@");
        assertThat(url.toLowerCase(java.util.Locale.ROOT)).doesNotContain("user=", "password=", "createdatabaseifnotexist=true");
        var source = new DriverManagerDataSource(url, System.getenv("MOA_TEST_DB_USER"), System.getenv("MOA_TEST_DB_PASSWORD"));
        var factory = mapperFactory(source);
        var start = LocalDateTime.of(2026, 11, 20, 0, 0);
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
            assertThat(connection.getCatalog()).matches("moa_join_test_[A-Za-z0-9_]+");
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                assertThat(tables.next()).isFalse();
            }
            var createdTables = new java.util.ArrayList<String>();
            try {
                statement.execute("CREATE TABLE `groups` (group_id BIGINT PRIMARY KEY, deleted_at DATETIME, max_members INT, member_id BIGINT) ENGINE=InnoDB");
                createdTables.add("`groups`");
                statement.execute("CREATE TABLE saving_group (saving_group_id BIGINT PRIMARY KEY, group_id BIGINT, status VARCHAR(2)) ENGINE=InnoDB");
                createdTables.add("saving_group");
                statement.execute("""
                        CREATE TABLE group_member (crew_member_id BIGINT PRIMARY KEY, group_id BIGINT,
                          member_id BIGINT, member_role VARCHAR(10), membership_status VARCHAR(10)) ENGINE=InnoDB
                        """);
                createdTables.add("group_member");
                statement.execute("""
                        CREATE TABLE group_event (schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY, group_id BIGINT,
                          writer_id BIGINT, title VARCHAR(100), event_date DATETIME) ENGINE=InnoDB
                        """);
                createdTables.add("group_event");
                statement.execute("INSERT INTO `groups` VALUES (10,NULL,10,30)");
                statement.execute("INSERT INTO saving_group VALUES (20,10,'02')");
                statement.execute("INSERT INTO group_member VALUES (1,10,30,'OWNER','APPROVED'),(2,10,31,'MANAGER','APPROVED')");
                try (var session = factory.openSession()) {
                    var mapper = session.getMapper(SavingGroupMapper.class);
                    for (int i = 0; i < 9; i++) mapper.insertCalendarEvent(new Insert(10L, 30L, "기존 일정", start));
                    session.commit();
                }

                var ready = new CountDownLatch(2);
                var executor = Executors.newFixedThreadPool(2);
                try {
                    var owner = executor.submit(register(factory, ready, 30L, start));
                    var manager = executor.submit(register(factory, ready, 31L, start));
                    assertThat(java.util.List.of(owner.get(15, TimeUnit.SECONDS), manager.get(15, TimeUnit.SECONDS)))
                            .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
                } finally {
                    executor.shutdownNow();
                    executor.awaitTermination(15, TimeUnit.SECONDS);
                }
                try (var session = factory.openSession()) {
                    var mapper = session.getMapper(SavingGroupMapper.class);
                    assertThat(mapper.countCalendarEvents(10L, start, start.plusDays(1))).isEqualTo(10);
                    // AI작성: 실제 행을 삭제한 뒤 새 트랜잭션에서 등록 한도가 회복되는지 확인한다.
                    Long id = mapper.selectCalendarEvents(10L, start, start.plusDays(1)).get(0).scheduleId();
                    new SavingGroupCalendarServiceImpl(mapper, org.mockito.Mockito.mock(com.nightstudy.moa.notification.service.NotificationService.class)).delete(20L, 31L, id);
                    assertThat(mapper.selectCalendarEvents(10L, start, start.plusDays(1)))
                            .noneMatch(row -> row.scheduleId().equals(id));
                    session.commit();
                }
                try (var session = factory.openSession()) {
                    var mapper = session.getMapper(SavingGroupMapper.class);
                    assertThat(mapper.countCalendarEvents(10L, start, start.plusDays(1))).isEqualTo(9);
                }
                try (var session = factory.openSession()) {
                    var mapper = session.getMapper(SavingGroupMapper.class);
                    new SavingGroupCalendarServiceImpl(mapper, org.mockito.Mockito.mock(com.nightstudy.moa.notification.service.NotificationService.class)).create(20L, 30L,
                            new CreateRequest("삭제 후 등록", start.toLocalDate(), LocalTime.NOON));
                    assertThat(mapper.countCalendarEvents(10L, start, start.plusDays(1))).isEqualTo(10);
                    session.commit();
                }
            } finally {
                for (String table : createdTables) statement.execute("DROP TABLE " + table);
            }
        }
    }

    private Callable<HttpStatus> register(SqlSessionFactory factory, CountDownLatch ready, Long memberId, LocalDateTime start) {
        return () -> {
            try (var session = factory.openSession(TransactionIsolationLevel.REPEATABLE_READ)) {
                ready.countDown();
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                var service = new SavingGroupCalendarServiceImpl(session.getMapper(SavingGroupMapper.class),
                        org.mockito.Mockito.mock(com.nightstudy.moa.notification.service.NotificationService.class));
                try {
                    service.create(20L, memberId, new CreateRequest("마지막 일정", start.toLocalDate(), LocalTime.NOON));
                    session.commit();
                    return HttpStatus.CREATED;
                } catch (ApiException error) {
                    session.rollback(true);
                    assertThat(error.getCode()).isEqualTo("SAVING_GROUP_DAILY_EVENT_LIMIT");
                    return HttpStatus.CONFLICT;
                }
            }
        };
    }

    private SqlSessionFactory mapperFactory(javax.sql.DataSource source) throws Exception {
        var config = new Configuration(new Environment("calendar-test", new JdbcTransactionFactory(), source));
        config.setMapUnderscoreToCamelCase(true);
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        String resource = "mappers/SavingGroupMapper.xml";
        try (var reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(reader, config, resource, config.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(config);
    }
}
