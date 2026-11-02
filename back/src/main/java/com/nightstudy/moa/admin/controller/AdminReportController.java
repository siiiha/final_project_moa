package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminReportDetailResponse;
import com.nightstudy.moa.admin.dto.AdminReportListItemResponse;
import com.nightstudy.moa.admin.dto.AdminReportSearchCondition;
import com.nightstudy.moa.admin.dto.AdminReportProcessRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final AdminReportService adminReportService;

    // AI작성: 관리자 신고 기능을 처리하는 Service를 생성자 주입으로 전달받는다.
    @Autowired
    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    // AI작성: 처리 상태·신고 유형·검색어·페이지 조건으로 신고 목록을 조회한다.
    // 조회 결과와 프론트엔드의 페이지 이동에 필요한 페이징 정보를 함께 반환한다.
    @GetMapping
    public ApiResponse<List<AdminReportListItemResponse>> getReportList(
            @Valid @ModelAttribute AdminReportSearchCondition condition){
        AdminReportListResult result = adminReportService.getReportList(condition);

        return ApiResponse.ok(
                result.getReport(),
                result.getPageMeta()
        );
    }

    // AI작성: 신고 사건 번호로 사건 정보와 해당 사건에 접수된 개별 신고 내용을 상세 조회한다.
    @GetMapping("/{caseId}")
    public ApiResponse<AdminReportDetailResponse> getReportDetail(
            @PathVariable Long caseId){
        AdminReportDetailResponse result = adminReportService.getReportDetail(caseId);

        return ApiResponse.ok(result);
    }

    // AI작성: 신고를 승인 또는 반려하고, 승인 시 신고 대상 회원에게 선택한 기간의 이용 제한을 적용한다.
    // 상태 변경과 회원 제한은 데이터 불일치를 막기 위해 Service의 한 트랜잭션에서 처리한다.
    @PatchMapping("/{reportId}/process")
    public ApiResponse<Void> processReport(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportProcessRequest request) {
        adminReportService.processReport(reportId, request);
        return ApiResponse.ok(null);
    }

    // AI작성: 검색 조건의 공백 문자열을 null로 변환하여 불필요한 조건이 Mapper에 전달되지 않게 한다.
    @InitBinder
    public void initBinder(WebDataBinder binder){
        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true)
        );
    }
}
