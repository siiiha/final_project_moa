package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// AI작성: 설정 조회와 저장이 같은 계모임 ID를 사용하고 저장 전 잠금을 유지하는지 검증한다.
class SavingGroupSettingsMapperTest {
    @ParameterizedTest
    @CsvSource({
            "selectSavingGroupSettingsAuthority, false",
            "selectSavingGroupCurrentSettings, false",
            "selectSavingGroupSettingsAuthorityForUpdate, true"
    })
    void settingsQueriesUseSavingGroupId(String statement, boolean locksTarget) throws Exception {
        var configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        String resource = "mappers/SavingGroupMapper.xml";
        try (var reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }

        var bound = configuration.getMappedStatement(SavingGroupMapper.class.getName() + "." + statement)
                .getBoundSql(Map.of("savingGroupId", 10L));
        String sql = bound.getSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("WHERE sg.saving_group_id = ?", "g.deleted_at IS NULL")
                .doesNotContain("WHERE sg.group_id = ?");
        assertThat(bound.getParameterMappings()).extracting(ParameterMapping::getProperty)
                .containsExactly("savingGroupId");
        assertThat(sql.endsWith("FOR UPDATE")).isEqualTo(locksTarget);
    }
}
