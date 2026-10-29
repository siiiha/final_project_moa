package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListItemResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSavingGroupRestrictionRequest;
import com.nightstudy.moa.admin.service.AdminSavingGroupService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/saving-groups")
// AI작성: SecurityConfig에서 ADMIN 권한을 확인한 계모임 목록·상세 요청을 처리한다.
public class AdminSavingGroupController {
    private final AdminSavingGroupService adminSavingGroupService;

    public AdminSavingGroupController(AdminSavingGroupService adminSavingGroupService) {
        this.adminSavingGroupService = adminSavingGroupService;
    }

    @GetMapping
    public ApiResponse<List<AdminSavingGroupListItemResponse>> getSavingGroupList(
            @Valid @ModelAttribute AdminSavingGroupSearchCondition condition
    ) {
        AdminSavingGroupListResult result = adminSavingGroupService.getSavingGroupList(condition);
        return ApiResponse.ok(result.getSavingGroups(), result.getPageMeta());
    }

    @GetMapping("/{savingGroupId}")
    public ApiResponse<AdminSavingGroupDetailResponse> getSavingGroupDetail(
            @PathVariable Long savingGroupId
    ) {
        return ApiResponse.ok(adminSavingGroupService.getSavingGroupDetail(savingGroupId));
    }

    @PatchMapping("/{savingGroupId}/restriction")
    // AI작성: 기간 없이 관리자 사유를 기록하고 진행 중 계모임을 활동 정지한다.
    public ApiResponse<Void> restrictSavingGroup(
            @PathVariable Long savingGroupId,
            @Valid @RequestBody AdminSavingGroupRestrictionRequest request
    ) {
        adminSavingGroupService.restrictSavingGroup(savingGroupId, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{savingGroupId}/restriction/release")
    // AI작성: 미해제 관리자 제한 이력이 있는 계모임만 정상 진행 상태로 복구한다.
    public ApiResponse<Void> releaseSavingGroupRestriction(
            @PathVariable Long savingGroupId,
            @Valid @RequestBody AdminSavingGroupRestrictionRequest request
    ) {
        adminSavingGroupService.releaseSavingGroupRestriction(savingGroupId, request);
        return ApiResponse.ok(null);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
