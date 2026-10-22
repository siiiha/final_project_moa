package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSummaryResponse;
import java.util.List;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    // AI작성: JWT 연결 전에는 공개 조회만 제공하며 클라이언트의 사용자 ID·역할은 받지 않는다.
    @GetMapping("/{id}")
    public ApiResponse<SavingGroupDetailResponse> detail(
            @PathVariable("id") @Positive(message = "계모임 ID는 1 이상이어야 합니다.") Long id) {
        return ApiResponse.ok(savingGroupService.getSavingGroupDetail(id, null));
    }

    // AI작성: 목록 배열과 페이징 정보를 공통 ApiResponse의 data·meta에 각각 담아 반환한다.
    @GetMapping
    public ApiResponse<List<SavingGroupSummaryResponse>> list(
            @Valid @ModelAttribute SavingGroupSearchCondition condition) {
        SavingGroupListResponse result = savingGroupService.getSavingGroupList(condition);
        return ApiResponse.ok(result.getItems(), result.getPageMeta());
    }
}
