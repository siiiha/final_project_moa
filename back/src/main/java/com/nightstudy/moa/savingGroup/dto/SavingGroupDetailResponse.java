package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공개 정보와 현재 사용자의 승인 역할만 담으며 개인정보는 포함하지 않는다.
@Getter
@AllArgsConstructor
public class SavingGroupDetailResponse {
    // AI작성: 상세 URL은 groupId를 사용하지만 가입 신청은 savingGroupId가 필요해 함께 반환한다.
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
    private final List<SavingGroupNoticeSummaryResponse> notices;
    private final String myRole;
}
