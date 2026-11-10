package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import java.util.Map;
import java.time.LocalDate;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.Test;

// AI작성: 실제 MyBatis 바인딩으로 ID 범위·전체 승인 회원·읽기 전용 계약을 확인한다. DB 실행은 별도 검증한다.
class SavingGroupTurnOrderMapperTest {
    @Test void recommendationReadsAllApprovedRolesWithoutFinancialOrPersonalPriority() throws Exception {
        var config = new Configuration();
        config.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (var reader = Resources.getResourceAsReader("mappers/SavingGroupMapper.xml")) {
            new XMLMapperBuilder(reader, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        // AI작성: 추천 버튼이 의존하는 실제 상세 조회의 유형 전달 누락을 방지한다.
        assertThat(config.getMappedStatement(prefix + "selectSavingGroupDetailBySavingGroupId")
                .getBoundSql(Map.of("savingGroupId", 99L)).getSql())
                .contains("sg.saving_group_id = ?", "sg.saving_group_type AS group_type");
        var context = config.getMappedStatement(prefix + "selectTurnOrderRecommendationContext");
        assertThat(context.getSqlCommandType()).isEqualTo(SqlCommandType.SELECT);
        assertThat(context.getBoundSql(Map.of("savingGroupId", 99L)).getSql())
                .contains("sg.saving_group_id = ?", "g.deleted_at IS NULL", "sg.start_date");
        var members = config.getMappedStatement(prefix + "selectTurnOrderRecommendationMembers");
        assertThat(members.getSqlCommandType()).isEqualTo(SqlCommandType.SELECT);
        var bound = members.getBoundSql(Map.of("groupId", 20L));
        assertThat(bound.getSql()).contains("gm.group_id = ?", "gm.membership_status = 'APPROVED'",
                "'OWNER', 'MANAGER', 'MEMBER'", "m.deleted_at IS NULL", "m.nickname",
                "gm.sequence_number", "pr.status = 'COMPLETED'")
                .doesNotContain("FOR UPDATE", "fee_payment", "m.name", "m.email");
        assertThat(bound.getParameterMappings()).extracting(p -> p.getProperty()).containsExactly("groupId");
        // AI작성: 현재 월 이하의 최신 예약을 쓰며 예약이 있으면 기본 순번으로 되돌리지 않는다.
        var running = members.getBoundSql(Map.of("groupId", 20L, "savingGroupId", 99L, "month", LocalDate.of(2026, 11, 1)));
        assertThat(running.getSql()).contains("effective_month <= ?", "ORDER BY effective_month DESC LIMIT 1",
                "THEN gm.sequence_number ELSE planned.round_number END", "planned.member_id = gm.member_id")
                .doesNotContain("FOR UPDATE");
        assertThat(running.getParameterMappings()).extracting(p -> p.getProperty())
                .containsExactly("savingGroupId", "month", "groupId");
    }
}
