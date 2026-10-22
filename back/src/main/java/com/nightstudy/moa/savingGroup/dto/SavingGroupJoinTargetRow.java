package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 조건을 검증할 계모임의 내부 모임 ID, 상태, 정원을 조회하는 MyBatis 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupJoinTargetRow {
    private Long savingGroupId;
    private Long groupId;
    private String status;
    private Integer maxMembers;
}
