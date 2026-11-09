package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 실제 XML을 파싱해 최초 기준 조회와 조건부 확인 소비의 SQL 범위를 확인한다.
class SavingGroupDepositMapperTest {
    @Test void keepsOriginalWindowAndConsumesOnlyOwnedPendingRequest() throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var anchor = config.getMappedStatement(prefix + "selectDepositAnchorForUpdate").getBoundSql(Map.of()).getSql();
        assertThat(anchor).contains("account_id = ?", "member_id = ?", "amount = ?", "window_anchor_request_id IS NULL",
                "completed_at > ?", "FOR UPDATE");
        var update = config.getMappedStatement(prefix + "completeDepositRequest").getBoundSql(Map.of()).getSql();
        assertThat(update).contains("member_id = ?", "account_id = ?", "status = 'PENDING_CONFIRMATION'", "confirmation_token_hash = NULL");
        var insert = config.getMappedStatement(prefix + "insertDepositTransaction").getBoundSql(Map.of()).getSql();
        assertThat(insert).contains("'DEPOSIT'", "'테스트 입금'", "balance_after").doesNotContain("account_number", "source_transaction_id");
    }
}
