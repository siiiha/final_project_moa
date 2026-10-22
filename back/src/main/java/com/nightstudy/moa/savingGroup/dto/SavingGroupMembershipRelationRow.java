package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 기존 모임원 관계의 상태를 구분해 중복 신청과 자진 탈퇴 후 재신청을 처리하는 MyBatis 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupMembershipRelationRow {
    private Long crewMemberId;
    private String memberRole;
    private String membershipStatus;
}
