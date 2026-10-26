package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 계주 전용 목록의 최소 표시 정보다. 이름은 SQL에서 마스킹하며 BIGINT ID는 문자열로 전달한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupJoinRequestRow {
    private String crewMemberId;
    private String applicant;
    private String regionName;
    private boolean identityVerified;
    private boolean regionVerified;
    private String applicationMessage;
    private String membershipStatus;
}
