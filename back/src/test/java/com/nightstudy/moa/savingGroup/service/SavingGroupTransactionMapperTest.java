package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.savingGroup.dto.*;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
// AI작성: 실제 MyBatis 바인딩으로 필터 공유·안정 정렬·공개 컬럼을 확인한다.
class SavingGroupTransactionMapperTest {
    @ParameterizedTest @ValueSource(strings={"LATEST", "OLDEST"})
    void bindsSharedConditionsAndStableOrder(String sort) throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        var condition = new SavingGroupTransactionSearchCondition();
        condition.setSort(sort); condition.setTransactionType("WITHDRAWAL");
        condition.setMinAmount(new BigDecimal("10000")); condition.setMaxAmount(new BigDecimal("40000"));
        var params = Map.of("accountId", 3L, "condition", condition, "offset", 10);
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        for (String name : new String[]{"selectSavingGroupTransactionCount", "selectSavingGroupTransactions"}) {
            String sql = config.getMappedStatement(prefix+name).getBoundSql(params).getSql().replaceAll("\\s+", " ");
            assertThat(sql).contains("account_id = ?", "transaction_type = ?", "amount >= ?", "amount <= ?")
                .doesNotContain("account_number", "description", "source_transaction_id");
        }
        String sql = config.getMappedStatement(prefix+"selectSavingGroupTransactions").getBoundSql(params).getSql();
        String direction = sort.equals("LATEST") ? "DESC" : "ASC";
        assertThat(sql).contains("transacted_at "+direction+", transaction_id "+direction, "LIMIT 10 OFFSET ?");
        condition.setTransactionType(null); condition.setMinAmount(null); condition.setMaxAmount(null);
        assertThat(config.getMappedStatement(prefix+"selectSavingGroupTransactionCount").getBoundSql(params).getSql())
            .doesNotContain("transaction_type =", "amount >=", "amount <=");
    }
}
