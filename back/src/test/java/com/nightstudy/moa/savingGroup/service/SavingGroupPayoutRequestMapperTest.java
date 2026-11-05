package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.HashMap;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 실제 MyBatis XML 바인딩에서 유형별 소속·탈퇴 조건·안정 정렬과 민감 필드 제외를 검증한다.
class SavingGroupPayoutRequestMapperTest {
    @ParameterizedTest @ValueSource(strings = {"순번계", "친목계"})
    void scopeAndPrivacyAreSharedByCountAndRows(String type) throws Exception {
        Configuration config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        var params = new HashMap<String, Object>();
        params.put("groupId", 20L); params.put("groupType", type); params.put("status", "PENDING"); params.put("offset", 10);
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        for (String name : new String[]{"selectSavingGroupPayoutRequests", "selectSavingGroupPayoutRequestCount"}) {
            String sql = config.getMappedStatement(prefix + name).getBoundSql(params).getSql().replaceAll("\\s+", " ");
            assertThat(sql).contains("group_id = ?", "requester.deleted_at IS NULL", "membership_status = 'APPROVED'", "req.status = ?")
                    .doesNotContain("transaction_id", "account_number", "DELETE FROM");
            assertThat(sql).contains(type.equals("순번계") ? "FROM payout_request" : "FROM expenses");
        }
        String rows = config.getMappedStatement(prefix + "selectSavingGroupPayoutRequests").getBoundSql(params).getSql();
        assertThat(rows).contains("LIMIT 10 OFFSET", "DESC");
        if (type.equals("순번계")) assertThat(rows).contains("reviewer.deleted_at IS NULL", "reviewer_relation.membership_status = 'APPROVED'");
        else assertThat(rows).contains("AS has_evidence").doesNotContain("AS receipt_img_url");
        params.put("status", null);
        assertThat(config.getMappedStatement(prefix + "selectSavingGroupPayoutRequestCount").getBoundSql(params).getSql())
                .doesNotContain("req.status =");
    }
}
