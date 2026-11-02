package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 계모임 관리 화면의 전체 현황 카드에 필요한 집계값을 제공한다.
public class AdminSavingGroupSummaryResponse {
    private long totalSavingGroups;
    private long activeSavingGroups;
    private long restrictedSavingGroups;
}
