package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.service.SavingGroupCreationService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI작성: 개설 API는 기존 URL을 유지하되 목록·상세 Controller와 빈 이름을 분리한다.
@RestController
@RequestMapping("/api/saving-group")
public class SavingGroupCreationController {

    private final SavingGroupCreationService savingGroupCreationService;

    public SavingGroupCreationController(SavingGroupCreationService savingGroupCreationService) {
        this.savingGroupCreationService = savingGroupCreationService;
    }

    @PostMapping
    public ApiResponse<String> applyForCreationSavingGroup(
            @ModelAttribute SavingGroupCreationRequest savingGroupCreationRequest
    ) {
        savingGroupCreationService.applyForCreationSavingGroup(savingGroupCreationRequest);
        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<String> approveForCreationSavingGroup(
            @PathVariable("id") Long savingGroupId
    ) {
        savingGroupCreationService.approveForCreationSavingGroup(savingGroupId);
        return ApiResponse.ok();
    }
}
