package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSummaryResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: /api 경로는 기존 프론트 클라이언트와 개발 프록시를 위한 동일 API 별칭이다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupController {
    private final SavingGroupService savingGroupService;

    // AI작성: 목록 조회 서비스만 주입한다.
    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    // AI작성: 게시글과 동일하게 data에 목록, meta에 공통 페이징 정보를 반환한다.
    @GetMapping
    public ApiResponse<List<SavingGroupSummaryResponse>> list(
            @Valid @ModelAttribute SavingGroupSearchCondition condition) {
        SavingGroupListResponse result = savingGroupService.getSavingGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }

    @GetMapping("/{savingGroupId}/settings")
    public ApiResponse<SavingGroupSettingsResponse> getSettings(
            @PathVariable Long savingGroupId) {
        // TODO: JWT 인증 완료 후 SecurityContext에서 로그인 회원 ID를 추출한다.
        Long loginMemberId = 67L;
        return ApiResponse.ok(savingGroupService.getSavingGroupSettings(savingGroupId, loginMemberId));
    }

    @PatchMapping("/{savingGroupId}")
    public ApiResponse<Void> updateSettings(
            @PathVariable Long savingGroupId,
            @Valid @RequestBody SavingGroupSettingsPatchRequest request) {
        // TODO: JWT 인증 완료 후 SecurityContext에서 로그인 회원 ID를 추출한다.
        Long loginMemberId = 67L;
        savingGroupService.updateSavingGroupSettings(savingGroupId, loginMemberId, request);
        return ApiResponse.ok(null);
    }
}
