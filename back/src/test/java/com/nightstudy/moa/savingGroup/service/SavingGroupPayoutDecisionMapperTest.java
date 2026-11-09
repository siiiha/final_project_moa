package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 실제 XML 분기를 바인딩해 소속·대기 조건과 처리 기록 및 자금 이동 SQL 부재를 확인한다.
class SavingGroupPayoutDecisionMapperTest {
    @ParameterizedTest @ValueSource(strings = {"순번계", "친목계"})
    void decisionUpdatesOnlyPendingScopedRequest(String type) throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        var params = Map.of("groupType", type, "groupId", 20L, "requestId", 40L,
                "status", "REJECTED", "reviewerId", 30L, "rejectReason", "증빙 확인 필요");
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var bound = config.getMappedStatement(prefix + "updatePayoutDecision").getBoundSql(params);
        assertThat(bound.getSql()).contains("reviewer_id", "reviewed_at = CURRENT_TIMESTAMP", "reject_reason", "group_id = ?", "'PENDING'")
                .doesNotContain("completed_at", "transaction_id", "balance", "sequence_number =");
        assertThat(bound.getParameterMappings()).extracting(p -> p.getProperty())
                .containsExactly("status", "reviewerId", "rejectReason", "requestId", "groupId");
        String read = config.getMappedStatement(prefix + "selectPayoutDecisionRequestForUpdate").getBoundSql(params).getSql();
        assertThat(read).contains("group_id = ?", "FOR UPDATE");
        assertThat(read).contains(type.equals("순번계") ? "payout_request_id" : "expense_id");
        assertThat(config.getMappedStatement(prefix + "selectPayoutDecisionGroupForUpdate").getBoundSql(Map.of("savingGroupId", 10L)).getSql())
                .contains("sg.saving_group_id = ?", "g.deleted_at IS NULL", "FOR UPDATE");
    }
}
