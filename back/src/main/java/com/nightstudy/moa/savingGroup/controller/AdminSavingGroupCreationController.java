package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListItemResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRejectionRequest;
import com.nightstudy.moa.savingGroup.service.SavingGroupCreationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: SecurityConfig에서 ADMIN 권한을 검증한 계모임 개설·삭제 심사 요청을 처리한다.
@RestController
@RequestMapping("/api/admin/saving-group-applications")
public class AdminSavingGroupCreationController {
    private final SavingGroupCreationService savingGroupCreationService;

    public AdminSavingGroupCreationController(SavingGroupCreationService savingGroupCreationService) {
        this.savingGroupCreationService = savingGroupCreationService;
    }

    @GetMapping
    public ApiResponse<List<SavingGroupCreationApplicationListItemResponse>> list(
            @Valid @ModelAttribute SavingGroupCreationApplicationSearchCondition condition) {
        PageMeta pageMeta = savingGroupCreationService.getCreationApplicationPageMeta(condition);
        return ApiResponse.ok(savingGroupCreationService.getCreationApplications(condition, pageMeta), pageMeta);
    }

    @GetMapping("/{approvalId}")
    public ApiResponse<SavingGroupCreationApplicationDetailResponse> detail(
            @PathVariable @Positive(message = "신청 번호는 1 이상이어야 합니다.") Long approvalId) {
        return ApiResponse.ok(savingGroupCreationService.getCreationApplication(approvalId));
    }

    @PatchMapping("/{approvalId}/approve")
    public ApiResponse<Void> approve(
            @PathVariable @Positive(message = "신청 번호는 1 이상이어야 합니다.") Long approvalId,
            @RequestAttribute("memberId") Long adminMemberId) {
        // AI작성: 승인자 정보는 요청 입력값이 아닌 JWT 필터가 검증한 관리자 회원 번호만 사용한다.
        savingGroupCreationService.approveCreationApplication(approvalId, adminMemberId);
        return ApiResponse.ok();
    }

    @PatchMapping("/{approvalId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable @Positive(message = "신청 번호는 1 이상이어야 합니다.") Long approvalId,
            @Valid @RequestBody SavingGroupCreationRejectionRequest request) {
        savingGroupCreationService.rejectCreationApplication(approvalId, request.getRejectReason());
        return ApiResponse.ok();
    }
}
