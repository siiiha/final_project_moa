package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 신청 회원 잠금 전 식별과 잠금 후 재검증에 사용하는 내부 조회 모델이다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupJoinApprovalTargetRow {
    private Long crewMemberId;
    private Long memberId;
    private Long groupId;
    private String membershipStatus;
}
