package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminDashboardSummaryResponse;
import com.nightstudy.moa.admin.service.AdminDashboardService;
import com.nightstudy.moa.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
// AI작성: 관리자 권한으로만 대시보드 회원 현황을 조회한다.
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryResponse> getDashboardSummary() {
        return ApiResponse.ok(adminDashboardService.getDashboardSummary());
    }
}
