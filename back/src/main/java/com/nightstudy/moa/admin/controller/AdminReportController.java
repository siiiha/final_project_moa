package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.service.AdminReportService;
import com.nightstudy.moa.admin.dto.AdminReportListResult;
import com.nightstudy.moa.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final AdminReportService adminReportService;

    @Autowired
    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    @GetMapping
    public ApiResponse<List<AdminReportListItemResponse>> getReportList(
            @ModelAttribute AdminReportSearchCondition condition){
        AdminReportListResult result = adminReportService.getReportList(condition);

        return ApiResponse.ok(
                result.getReport(),
                result.getPageMeta()
        );
    }

    @GetMapping("/{caseId}")
    public ApiResponse<AdminReportDetailResponse> getReportDetail(
            @PathVariable Long caseId){
        AdminReportDetailResponse result = adminReportService.getReportDetail(caseId);

        return ApiResponse.ok(result);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder){
        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true)
        );
    }
}
