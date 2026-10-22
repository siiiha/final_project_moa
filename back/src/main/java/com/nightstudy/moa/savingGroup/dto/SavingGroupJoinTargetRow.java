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
    // AI작성: 모임 생성 회원과 승인된 OWNER 관계를 함께 확인할 때 사용한다.
    private Long ownerMemberId;
}
