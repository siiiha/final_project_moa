package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 실제 Mapper XML의 중복 범위·상태·생성 키·금융 원장 미변경 계약을 검증한다. DB 실행 검증은 별도다.
class SavingGroupPayoutMapperTest {
    @Test void payoutStatementsPreserveRequestOnlyContract() throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var parameters = Map.of("groupId", 1L, "roundId", 2L, "hash", "a".repeat(64));
        assertThat(config.getMappedStatement(prefix + "selectPayoutTargetForUpdate").getBoundSql(parameters).getSql())
                .contains("deleted_at IS NULL FOR UPDATE");
        assertThat(config.getMappedStatement(prefix + "existsBlockingRoundPayout").getBoundSql(parameters).getSql())
                .contains("round_id = ?", "'PENDING', 'APPROVED', 'COMPLETED'").doesNotContain("'REJECTED'");
        assertThat(config.getMappedStatement(prefix + "existsBlockingReceiptExpense").getBoundSql(parameters).getSql())
                .contains("group_id = ? AND receipt_sha256 = ?", "'PENDING', 'APPROVED'").doesNotContain("'REJECTED'");
        for (String name : new String[]{"insertRoundPayout", "insertReceiptExpense"}) {
            var statement = config.getMappedStatement(prefix + name);
            assertThat(statement.getKeyProperties()).containsExactly("id");
            assertThat(statement.getBoundSql(parameters).getSql()).contains("'PENDING'")
                    .doesNotContain("group_transaction", "UPDATE", "COMPLETED");
        }
    }
}
