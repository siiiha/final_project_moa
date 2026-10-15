package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportDetailResult;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionItem;

public interface AdminReportService {
    AdminReportListResult getReportList(
            AdminReportSearchCondition condition
    );

    AdminReportDetailResult getReportDetail(
            Long caseId
    );
}
