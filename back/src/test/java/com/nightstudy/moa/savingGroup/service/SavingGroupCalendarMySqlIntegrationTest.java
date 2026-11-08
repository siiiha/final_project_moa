package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.Insert;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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
            var config = new Configuration(new Environment("calendar-test", new JdbcTransactionFactory(),
                    new SingleConnectionDataSource(connection, true)));
            config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
            String resource = "mappers/SavingGroupMapper.xml";
            try (var reader = Resources.getResourceAsReader(resource)) {
                new XMLMapperBuilder(reader, config, resource, config.getSqlFragments()).parse();
            }
            var factory = new SqlSessionFactoryBuilder().build(config);
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
                assertThat(session.getMapper(SavingGroupMapper.class).selectCalendarEvents(10L, start, end))
                        .singleElement().satisfies(row -> {
                            assertThat(row.scheduleId()).isEqualTo(event.scheduleId);
                            assertThat(row.title()).isEqualTo("정기 모임");
                            assertThat(row.eventDate()).isEqualTo(start);
                        });
            }
        }
    }
}
