package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.VirtualAccountIssueRecord;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

// AI작성: 실제 Mapper XML을 파싱해 상태 조건과 개인정보 없는 이력 SQL을 확인한다.
class VirtualAccountStorageMapperTest {
    @Test void bindsIssueAndConditionalCompletion() throws Exception {
        Configuration config = new Configuration();
        try (var input = Resources.getResourceAsStream("mappers/VirtualAccountStorageMapper.xml")) {
            new XMLMapperBuilder(input, config, "mappers/VirtualAccountStorageMapper.xml", config.getSqlFragments()).parse();
        }
        String ns = "com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper.";
        var issue = new VirtualAccountIssueRecord();
        issue.setApprovalId(1L); issue.setGroupId(2L); issue.setOrderId("moa_test");
        var insert = config.getMappedStatement(ns + "insertIssue").getBoundSql(issue);
        assertThat(insert.getParameterMappings()).hasSize(3);
        assertThat(insert.getSql()).doesNotContain("account_number");
        var complete = config.getMappedStatement(ns + "completeIssue")
                .getBoundSql(Map.of("approvalId", 1L, "accountId", 2L));
        assertThat(complete.getSql()).contains("status = 'REQUESTED'", "status = 'COMPLETED'");
        assertThat(config.getMappedStatement(ns + "selectAccount").getBoundSql(Map.of("groupId", 2L)).getSql())
                .contains("WHERE group_id = ?");
    }
}
