package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

// AI작성: 공용 계좌 범위와 페이지 정렬 조건을 실제 MyBatis 바인딩으로 확인한다.
class SavingGroupTransactionMapperTest {
    @Test
    void bindsAccountAndStablePageOrder() throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        String resource = "mappers/SavingGroupMapper.xml";
        try (var reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(reader, config, resource, config.getSqlFragments()).parse();
        }
        var params = Map.of("accountId", 3L, "offset", 10L, "size", 10);
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        String count = config.getMappedStatement(prefix + "selectSavingGroupTransactionCount").getBoundSql(params).getSql();
        String list = config.getMappedStatement(prefix + "selectSavingGroupTransactions").getBoundSql(params).getSql();
        assertThat(count).contains("WHERE account_id = ?");
        assertThat(list).contains("WHERE account_id = ?", "transacted_at DESC, transaction_id DESC", "LIMIT ? OFFSET ?")
                .doesNotContain("source_transaction_id", "account_number");
    }
}
