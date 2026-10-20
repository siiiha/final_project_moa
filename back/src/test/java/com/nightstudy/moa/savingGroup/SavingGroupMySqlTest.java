package com.nightstudy.moa.savingGroup;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListCondition;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

// AI작성: 별도 테스트 DB를 명시했을 때만 실제 MySQL에서 실행하며 임시 테이블만 사용한다.
@EnabledIfEnvironmentVariable(named = "MOA_TEST_MYSQL_URL", matches = ".+")
class SavingGroupMySqlTest {
    private Connection connection;
    private SqlSession session;
    private SavingGroupMapper mapper;

    // AI작성: 운영 설정을 읽지 않고 연결별 임시 테이블로 조회 기능을 검증한다.
    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("MOA_TEST_MYSQL_URL"),
                System.getenv("MOA_TEST_MYSQL_USER"), System.getenv("MOA_TEST_MYSQL_PASSWORD"));
        connection.setAutoCommit(false);
        execute("CREATE TEMPORARY TABLE region (region_id BIGINT PRIMARY KEY, region_name VARCHAR(100)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        execute("CREATE TEMPORARY TABLE `groups` (group_id BIGINT PRIMARY KEY, region_id BIGINT, group_name VARCHAR(100), description VARCHAR(50), max_members INT, created_at DATETIME, deleted_at DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        execute("CREATE TEMPORARY TABLE saving_group (saving_group_id BIGINT PRIMARY KEY, group_id BIGINT, dues_amount DECIMAL(12,2), dues_day TINYINT, status VARCHAR(20)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        execute("CREATE TEMPORARY TABLE group_member (crew_member_id BIGINT PRIMARY KEY, group_id BIGINT, member_id BIGINT, membership_status VARCHAR(20)) ENGINE=InnoDB");
        execute("INSERT INTO region VALUES (1, '서울'), (2, '경기')");
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        try (Reader reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, configuration, "mappers/SavingGroupMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(connection);
        mapper = session.getMapper(SavingGroupMapper.class);
    }

    // AI작성: 임시 데이터만 롤백하고 연결 종료 시 임시 테이블을 제거한다.
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) connection.rollback();
        if (session != null) session.close();
        if (connection != null && !connection.isClosed()) connection.close();
    }

    // AI작성: 실제 MySQL에서 0·1·6·7건의 페이지 크기와 동률 정렬을 확인한다.
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 6, 7})
    void pageSizesAndStableOrder(int size) throws Exception {
        for (int id = 1; id <= size; id++) insertGroup(id, "모임", "소개", "01", 1, false);
        var first = new SavingGroupListCondition(null, null, 0, 6);
        assertEquals(size, mapper.selectSavingGroupCount(first));
        var rows = mapper.selectSavingGroupList(first);
        assertEquals(Math.min(size, 6), rows.size());
        if (size > 0) assertEquals(size, rows.get(0).getSavingGroupId());
        assertEquals(Math.max(0, size - 6), mapper.selectSavingGroupList(
                new SavingGroupListCondition(null, null, 6, 6)).size());
    }

    // AI작성: 상태·삭제·지역 조건과 승인 회원 중복 제거 및 무계원 모임 보존을 확인한다.
    @Test
    void visibilityFiltersAndMemberCounts() throws Exception {
        insertGroup(1, "모임", "소개", "01", 1, false);
        insertGroup(2, "모임", null, "02", 2, false);
        insertGroup(3, "모임", "소개", "00", 1, false);
        insertGroup(4, "모임", "소개", "03", 1, false);
        insertGroup(5, "모임", "소개", "04", 1, false);
        insertGroup(6, "모임", "소개", "01", 1, true);
        execute("INSERT INTO group_member VALUES (1,1,10,'APPROVED'),(2,1,10,'APPROVED'),(3,1,11,'APPROVED'),(4,1,12,'PENDING'),(5,1,13,'REJECTED'),(6,1,14,'LEFT')");
        var all = new SavingGroupListCondition(null, null, 0, 6);
        assertEquals(2, mapper.selectSavingGroupCount(all));
        assertEquals(2, mapper.selectSavingGroupList(all).size());
        assertEquals(1, mapper.selectSavingGroupCount(new SavingGroupListCondition(null, "서울", 0, 6)));
        var counts = mapper.selectApprovedMemberCounts(java.util.List.of(1L, 2L));
        assertEquals(1, counts.size());
        assertEquals(2, counts.get(0).getMemberCount());
    }

    // AI작성: 제목·소개글 OR 검색과 특수문자·대소문자 정책을 실제 LIKE로 확인한다.
    @Test
    void literalSearchAndCaseInsensitiveOr() throws Exception {
        insertGroup(1, "AbC", "소개", "01", 1, false);
        insertGroup(2, "모임", "ABC", "01", 1, false);
        insertGroup(3, "abc", "abc", "01", 2, false);
        insertGroup(4, "100%_!", "소개", "01", 1, false);
        assertEquals(3, mapper.selectSavingGroupCount(new SavingGroupListCondition("%abc%", null, 0, 6)));
        var combined = new SavingGroupListCondition("%abc%", "서울", 0, 6);
        assertEquals(2, mapper.selectSavingGroupCount(combined));
        assertEquals(2, mapper.selectSavingGroupList(combined).size());
        assertEquals(1, mapper.selectSavingGroupCount(new SavingGroupListCondition("%!%!_!!%", null, 0, 6)));
    }

    // AI작성: 외부 입력을 SQL에 결합하지 않는 가상 테스트 데이터 생성이다.
    private void insertGroup(int id, String name, String description, String status, int region, boolean deleted) throws Exception {
        try (var statement = connection.prepareStatement("INSERT INTO `groups` VALUES (?, ?, ?, ?, 8, '2026-10-20 12:00:00', ?)")) {
            statement.setInt(1, id); statement.setInt(2, region);
            statement.setString(3, name); statement.setString(4, description);
            statement.setString(5, deleted ? "2026-10-20 13:00:00" : null);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("INSERT INTO saving_group VALUES (?, ?, 50000, 25, ?)")) {
            statement.setInt(1, id); statement.setInt(2, id); statement.setString(3, status);
            statement.executeUpdate();
        }
    }

    // AI작성: 테스트 내부 고정 SQL만 실행한다.
    private void execute(String sql) throws Exception {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }
}
