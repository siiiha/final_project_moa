package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// AI작성: 실제 MyBatis 바인딩으로 목록 범위·개인정보·페이징 계약을 확인한다. DB 실행 테스트는 아니다.
class SavingGroupMemberMapperTest {
    Configuration configuration;
    final String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
    @BeforeEach void parse() throws Exception {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, configuration, "mappers/SavingGroupMapper.xml", configuration.getSqlFragments()).parse();
        }
    }
    @Test void countAndRowsShareScopeAndLimit() {
        var params = Map.of("groupId", 20L, "offset", 10);
        for (String statement : new String[]{"selectSavingGroupMemberCount", "selectSavingGroupMembers"}) {
            String sql = configuration.getMappedStatement(prefix + statement).getBoundSql(params).getSql();
            assertThat(sql).contains("gm.group_id = ?", "gm.membership_status = 'APPROVED'",
                    "gm.member_role IN ('MEMBER', 'MANAGER')", "m.deleted_at IS NULL");
        }
        assertThat(configuration.getMappedStatement(prefix + "selectSavingGroupMemberCount").getBoundSql(params).getSql())
                .contains("LEAST(COUNT(*), 20)");
        var rows = configuration.getMappedStatement(prefix + "selectSavingGroupMembers").getBoundSql(params);
        assertThat(rows.getSql()).contains("AS member_id, m.name", "LEFT(m.email, 1)", "gm.joined_at",
                "ORDER BY gm.joined_at DESC, gm.crew_member_id DESC", "LIMIT 10 OFFSET ?");
        assertThat(rows.getParameterMappings()).extracting(p -> p.getProperty()).containsExactly("groupId", "offset");
    }
    // AI작성: 검색은 이름·원문 이메일의 OR 조건으로 묶고 count와 rows의 바인딩을 동일하게 유지한다.
    @Test void searchIsBoundAndSharesCountScope() {
        var params = Map.of("groupId", 20L, "offset", 0, "keywordPattern", "%!%!_!!'검색%");
        for (String statement : new String[]{"selectSavingGroupMemberCount", "selectSavingGroupMembers"}) {
            var bound = configuration.getMappedStatement(prefix + statement).getBoundSql(params);
            assertThat(bound.getSql().replaceAll("\\s+", " "))
                    .contains("AND (m.name LIKE ? ESCAPE '!' OR m.email LIKE ? ESCAPE '!')")
                    .contains("gm.group_id = ?", "m.deleted_at IS NULL")
                    .doesNotContain("!%!_!!'검색");
            assertThat(bound.getParameterMappings()).extracting(p -> p.getProperty())
                    .containsSubsequence("groupId", "keywordPattern", "keywordPattern");
        }
        var plain = configuration.getMappedStatement(prefix + "selectSavingGroupMembers")
                .getBoundSql(Map.of("groupId", 20L, "offset", 0)).getSql();
        assertThat(plain).doesNotContain("m.name LIKE", "m.email LIKE");
    }
    // AI작성: 역할 갱신의 소속·현재 상태·기존 역할 조건과 회계 현재 읽기 잠금을 확인한다.
    @Test void roleUpdateProtectsScopeAndOwner() {
        var params = Map.of("groupId", 20L, "crewMemberId", 40L, "previousRole", "MEMBER", "role", "MANAGER");
        var update = configuration.getMappedStatement(prefix + "updateSavingGroupMemberRole").getBoundSql(params);
        assertThat(update.getSql()).contains("m.deleted_at IS NULL", "SET gm.member_role = ?",
                "gm.group_id = ? AND gm.crew_member_id = ?", "gm.membership_status = 'APPROVED'",
                "gm.member_role = ? AND gm.member_role IN ('MEMBER', 'MANAGER')", "AND ? IN ('MEMBER', 'MANAGER')");
        assertThat(update.getParameterMappings()).extracting(p -> p.getProperty())
                .containsExactly("role", "groupId", "crewMemberId", "previousRole", "role");
        assertThat(configuration.getMappedStatement(prefix + "selectSavingGroupManagerIdsForUpdate").getBoundSql(params).getSql())
                .contains("group_id = ? AND membership_status = 'APPROVED' AND member_role = 'MANAGER'", "FOR UPDATE");
    }
}
