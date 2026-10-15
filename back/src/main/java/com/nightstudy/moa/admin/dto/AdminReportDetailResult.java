package com.nightstudy.moa.admin.dto;

import java.util.List;

public class AdminReportDetailResult {
    // 신고 유형, 대상 회원, 처리 상태 등 신고 사건 정보
    private final List<AdminReportSubmissionItem> submissions;
    // 해당 사건에 접수된 신고자별 신고 내용
    private final AdminReportListItem reportCase;

}
