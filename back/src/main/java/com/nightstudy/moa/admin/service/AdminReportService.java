package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResult;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionItem;

public interface AdminReportService {
    // 검색·필터·페이징이 적용된 신고 사건 목록 조회
    AdminReportListResult getReportList(
            AdminReportSearchCondition condition
    );

    // 신고 사건과 해당 사건에 접수된 개별 신고 내용 조회
    AdminReportDetailResult getReportDetail(
            Long caseId
    );
}
