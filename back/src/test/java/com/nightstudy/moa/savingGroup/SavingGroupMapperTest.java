package com.nightstudy.moa.savingGroup;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListCondition;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// AI작성: MyBatis가 XML을 실제 파싱하도록 하여 조건 공유·바인딩과 집계 SQL을 검사한다.
class SavingGroupMapperTest {
    private Configuration configuration;
    private static final String NS = SavingGroupMapper.class.getName() + ".";

    // AI작성: DB 없이 실제 매퍼 XML과 결과 타입 매핑을 로딩한다.
    @BeforeEach
    void loadMapper() throws Exception {
        configuration = new Configuration();
        try (Reader reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, configuration, "mappers/SavingGroupMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
    }

    // AI작성: 목록·개수의 조건이 같으며 검색 입력이 SQL 본문에 치환되지 않는지 확인한다.
    @Test
    void sharesFiltersAndBindsSearchAndRegion() {
        var condition = new SavingGroupListCondition("%literal!%!_%", "서울", 6, 6);
        var count = configuration.getMappedStatement(NS + "selectSavingGroupCount").getBoundSql(condition);
        var list = configuration.getMappedStatement(NS + "selectSavingGroupList").getBoundSql(condition);
        String countSql = compact(count.getSql());
        String listSql = compact(list.getSql());
        assertEquals(countSql.substring(countSql.indexOf("FROM")),
                listSql.substring(listSql.indexOf("FROM"), listSql.indexOf("ORDER BY")).trim());
        assertTrue(listSql.contains("g.deleted_at IS NULL"));
        assertTrue(listSql.contains("sg.status IN ('01', '02')"));
        assertTrue(listSql.contains("g.group_name LIKE ? ESCAPE '!' OR g.description LIKE ? ESCAPE '!'"));
        assertTrue(listSql.contains("r.region_name = ?"));
        assertFalse(listSql.contains("literal"));
        assertTrue(listSql.contains("ORDER BY g.created_at DESC, sg.saving_group_id DESC LIMIT ? OFFSET ?"));
        assertEquals(5, list.getParameterMappings().size());
    }

    // AI작성: 검색·지역 미선택 시 LIKE나 지역 조건을 만들지 않는다.
    @Test
    void omitsOptionalFilters() {
        String sql = compact(configuration.getMappedStatement(NS + "selectSavingGroupList")
                .getBoundSql(new SavingGroupListCondition(null, null, 0, 6)).getSql());
        assertFalse(sql.contains("LIKE"));
        assertFalse(sql.contains("r.region_name ="));
    }

    // AI작성: 모든 모임을 조인하지 않고 페이지 ID만 승인 회원 중복 제거 집계한다.
    @Test
    void aggregatesApprovedDistinctMembersInOneQuery() {
        var bound = configuration.getMappedStatement(NS + "selectApprovedMemberCounts")
                .getBoundSql(Map.of("groupIds", List.of(1L, 2L)));
        String sql = compact(bound.getSql());
        assertTrue(sql.contains("COUNT(DISTINCT member_id)"));
        assertTrue(sql.contains("membership_status = 'APPROVED'"));
        assertEquals(2, bound.getParameterMappings().size());
    }

    // AI작성: 의미 없는 공백 차이를 제거한다.
    private String compact(String sql) { return sql.replaceAll("\\s+", " ").trim(); }

    // AI작성: 실제 XML이 group_id·공개 상태·승인 조건·최신 네 건과 제목 전용 조회를 구성하는지 검사한다.
    @Test
    void detailQueriesUsePublicAndApprovedFilters() {
        String detail = compact(configuration.getMappedStatement(NS + "selectSavingGroupDetail")
                .getBoundSql(Map.of("groupId", 42L)).getSql());
        assertTrue(detail.contains("WHERE g.group_id = ?"));
        assertTrue(detail.contains("g.deleted_at IS NULL AND sg.status IN ('01', '02')"));
        String notices = compact(configuration.getMappedStatement(NS + "selectRecentNotices")
                .getBoundSql(Map.of("groupId", 42L)).getSql());
        assertEquals("SELECT notice_id, title FROM group_notice WHERE group_id = ? ORDER BY created_at DESC, notice_id DESC LIMIT 4", notices);
        String count = compact(configuration.getMappedStatement(NS + "selectDetailMemberCount")
                .getBoundSql(Map.of("groupId", 42L)).getSql());
        assertTrue(count.contains("COUNT(DISTINCT member_id)"));
        assertTrue(count.contains("membership_status = 'APPROVED'"));
        String roles = compact(configuration.getMappedStatement(NS + "selectApprovedMemberRoles")
                .getBoundSql(Map.of("groupId", 42L, "memberId", 9L)).getSql());
        assertTrue(roles.contains("group_id = ? AND member_id = ? AND membership_status = 'APPROVED'"));
    }
}
