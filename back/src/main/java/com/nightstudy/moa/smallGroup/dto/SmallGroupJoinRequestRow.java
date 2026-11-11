package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 개설자 전용 목록의 applicant에는 실명 대신 닉네임만 제공하며 신청 ID는 문자열로 전달한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinRequestRow {
    private String crewMemberId;
    private String applicant;
    private String regionName;
    private boolean identityVerified;
    private boolean regionVerified;
    private String membershipStatus;
}
