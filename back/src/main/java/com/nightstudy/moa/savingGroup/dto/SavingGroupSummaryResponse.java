package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 상세 URL은 groupId를 사용하며 기존 savingGroupId도 호환성을 위해 유지한다.
// AI작성: 공개 카드 필드만 반환하고 내부 집계용 groupId는 노출하지 않는다.
@Getter
@AllArgsConstructor
public class SavingGroupSummaryResponse {
    private final Long savingGroupId;
    private final Long groupId;
    private final String groupName;
    private final String description;
    private final String regionName;
    private final Integer maxMembers;
    private final long memberCount;
    private final BigDecimal duesAmount;
    private final Integer duesDay;
    private final String status;
}
