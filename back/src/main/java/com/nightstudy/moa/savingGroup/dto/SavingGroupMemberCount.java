package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 페이지 내 모임별 승인 계원 집계 결과를 받는다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupMemberCount {
    private Long groupId;
    private long memberCount;
}
