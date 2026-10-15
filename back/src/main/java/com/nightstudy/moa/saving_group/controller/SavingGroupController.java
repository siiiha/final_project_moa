package com.nightstudy.moa.saving_group.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.saving_group.service.SavingGroupService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saving-group")
public class SavingGroupController {

    private final SavingGroupService savingGroupService;

    public SavingGroupController(SavingGroupService savingGroupService) {
        this.savingGroupService = savingGroupService;
    }

    @PostMapping("/create")
    public ApiResponse<String> applyForCreationSavingGroup (
            SavingGroupCreationRequest savingGroupCreationRequest
    ) {
        savingGroupService.applyForCreationSavingGroup(savingGroupCreationRequest);

        // TODO: ApiResponse로 응답 구현
        return null;
    }


}
