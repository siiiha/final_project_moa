package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 식별·잠금 재조회에서 신청 회원·소속·처리 상태를 확인하는 내부 조회 모델이다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinApprovalTargetRow {
    private Long crewMemberId;
    private Long memberId;
    private Long groupId;
    private String membershipStatus;
}
