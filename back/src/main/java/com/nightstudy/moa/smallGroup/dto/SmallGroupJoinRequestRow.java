package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 개설자 전용 가입 신청 목록의 최소 표시 정보다. 이름은 Mapper SQL에서 마스킹하고 신청 ID는 문자열로 전달한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinRequestRow {
    private String crewMemberId;
    private String applicant;
    private String regionName;
    private boolean identityVerified;
    private boolean regionVerified;
    private String applicationMessage;
    private String membershipStatus;
}
