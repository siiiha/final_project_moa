package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 승인된 계주에게 합의된 이름 원문과 닉네임을 제공하며 BIGINT ID는 문자열로 전달한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupJoinRequestRow {
    private String crewMemberId;
    private String applicant;
    private String nickname;
    private String regionName;
    private boolean identityVerified;
    private boolean regionVerified;
    private String applicationMessage;
    private String membershipStatus;
}
