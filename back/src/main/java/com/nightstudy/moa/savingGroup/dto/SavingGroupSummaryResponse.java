package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.Getter;

// AI작성: 카드의 상세 주소에 계모임 ID를 제공하고 응답의 공통 모임 ID도 유지한다.
@Getter
@RequiredArgsConstructor
public class SavingGroupSummaryResponse {
    // AI작성: 기존 생성자를 유지하며 공개 이미지 주소를 전달한다.
    @lombok.Setter
    private String groupImageUrl;
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
