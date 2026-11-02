package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 대상 소모임의 공통 모임 식별자·개설자·정원을 Service에서 검증하는 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinTargetRow {
    private Long smallGroupId;
    private Long groupId;
    private Long creatorMemberId;
    private Integer maxMembers;
}
