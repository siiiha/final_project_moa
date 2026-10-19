package com.nightstudy.moa.saving_group.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.saving_group.service.SavingGroupService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saving-group")
public class SavingGroupController {

    private final SavingGroupService savingGroupService;

    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    /**
     * 계모임 개설 신청 시 DB에 "심사중" 상태로 계모임 테이블에 데이터 생성하는 api
     *
     * @param savingGroupCreationRequest
     * @return
     */
    @PostMapping
    public ApiResponse<String> applyForCreationSavingGroup(
            @ModelAttribute SavingGroupCreationRequest savingGroupCreationRequest
    ) {
        boolean result = savingGroupService.applyForCreationSavingGroup(savingGroupCreationRequest);

        return ApiResponse.ok();
    }

    @PatchMapping("/{id}/approve")
    public ApiResponse<String> approveForCreationSavingGroup(
            @PathVariable("id") Long savingGroupId
    ) {
        boolean result = savingGroupService.approveForCreationSavingGroup(savingGroupId);

        return ApiResponse.ok();
    }

}
