package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminReportListItem;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportSubmissionItem;
import com.nightstudy.moa.admin.service.AdminReportService;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;



@RestController
public class AdminReportController {
    private final AdminReportService adminReportService;

    @Autowired
    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ApiResponse<List<AdminReportListItem>> getReportList(
            @ModelAttribute AdminReportSearchCondition condition){
        AdminReportListResult result = adminReportService.getReportList(condition);

        return ApiResponse.ok(
                result.getReport(),
                result.getPageMeta()
        );
    }

    @GetMapping
    public ApiResponse<List<AdminReportSubmissionItem>> getReportSubmission(
            @M
    )


}
