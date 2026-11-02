package com.nightstudy.moa.report.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.report.dto.ReportCreateRequest;
import com.nightstudy.moa.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
// AI작성: 로그인 회원의 게시글·댓글·회원 신고 접수를 처리한다.
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ApiResponse<Long> createReport(
            @RequestAttribute("memberId") Long reporterId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        return ApiResponse.ok(reportService.createReport(reporterId, request));
    }
}
