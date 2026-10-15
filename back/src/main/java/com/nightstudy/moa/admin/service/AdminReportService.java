package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;

public interface AdminReportService {
    AdminReportListResult getReportList(
            AdminReportSearchCondition condition
    );
}
