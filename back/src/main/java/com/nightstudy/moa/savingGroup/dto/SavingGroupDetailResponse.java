package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Getter;

// AI작성: 공개 정보와 현재 사용자의 승인 역할만 담으며 개인정보는 포함하지 않는다.
@Getter
@RequiredArgsConstructor
public class SavingGroupDetailResponse {
    // AI작성: 기존 생성자 호환을 유지하며 Service가 조회한 유형을 전달한다.
    @Setter
    private String groupType;
    // AI작성: 상세 URL에는 계모임 ID를 사용하고 공통 모임 신고에는 공통 모임 ID를 사용한다.
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
    // AI작성: 본인의 신청 상태와 서버 판단만 제공하며 다른 회원 정보는 포함하지 않는다.
    private final String membershipStatus;
    private final boolean canApply;
    private final String joinRestrictionCode;
    private final String joinRestrictionMessage;
    // AI작성: 기존 필수 필드 생성자는 유지하고 Service가 일반 목록과 별도로 고정 공지를 조합한다.
    @Setter
    private List<SavingGroupNoticeSummaryResponse> pinnedNotices = List.of();
}
