package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 로그인 사용자의 모든 소모임 관계를 그대로 조회해 중복과 MANAGER 오류를 Service에서 판단한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupMemberRelationRow {
    private String memberRole;
    private String membershipStatus;
}
