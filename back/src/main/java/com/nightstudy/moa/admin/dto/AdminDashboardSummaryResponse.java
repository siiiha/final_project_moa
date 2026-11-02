package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 대시보드 회원 현황 카드에 필요한 집계 값만 제공한다.
public class AdminDashboardSummaryResponse {
    private long totalMembers;
    private long activeMembers;
    private long newMembersThisMonth;
    private long managedMembers;
}
