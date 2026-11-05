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
}
