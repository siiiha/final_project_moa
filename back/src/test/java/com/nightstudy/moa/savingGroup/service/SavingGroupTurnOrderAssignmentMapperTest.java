package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 예약 적용월·모임 소속·지급 완료 제외에 필요한 실제 매핑 SQL을 검증한다.
class SavingGroupTurnOrderAssignmentMapperTest {
    @Test void scopedPlansAndMembersUseLocksAndCompletedPayouts() throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias("SavingGroupCreationRequest", com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest.class);
        try (var stream = Resources.getResourceAsStream("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(stream, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        assertThat(config.getMappedStatement(prefix + "selectTurnOrderPlan")
                .getBoundSql(Map.of("savingGroupId", 1L, "month", LocalDate.of(2026, 12, 1))).getSql())
                .contains("saving_group_id = ?", "effective_month <= ?", "ORDER BY effective_month DESC", "FOR UPDATE");
        assertThat(config.getMappedStatement(prefix + "selectTurnOrderAssignmentMembers")
                .getBoundSql(Map.of("groupId", 2L)).getSql())
                .contains("mr.group_id = gm.group_id", "pr.status = 'COMPLETED'", "membership_status = 'APPROVED'", "m.deleted_at IS NULL", "FOR UPDATE");
    }
}
