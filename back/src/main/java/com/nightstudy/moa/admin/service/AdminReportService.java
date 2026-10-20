package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;

// AI작성: 관리자 신고 조회와 처리 기능의 사용 규약을 정의한다.
public interface AdminReportService {
    // AI작성: 검색·필터·페이징이 적용된 신고 사건 목록을 조회한다.
    AdminReportListResult getReportList(
            AdminReportSearchCondition condition
    );

    // AI작성: 신고 사건과 해당 사건에 접수된 개별 신고 내용을 조회한다.
    AdminReportDetailResponse getReportDetail(
            Long caseId
    );

    // AI작성: 신고를 처리하고 필요한 경우 신고 대상 회원에게 이용 제한을 적용한다.
    void processReport(Long reportId, AdminReportProcessRequest request);
}
