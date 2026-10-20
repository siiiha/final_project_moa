package com.nightstudy.moa.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class AdminReportDetailResponse {
    // 신고 유형, 대상 회원, 처리 상태 등 신고 사건 정보
    private final AdminReportListItemResponse reportCase;
    // 해당 사건에 접수된 신고자별 신고 내용
    private final List<AdminReportSubmissionResponse> submissions;
}
