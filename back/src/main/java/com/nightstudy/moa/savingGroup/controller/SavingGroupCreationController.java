package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.service.SavingGroupCreationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
    public ApiResponse<Void> applyForCreationSavingGroup(
            @RequestAttribute("memberId") Long memberId,
            @Valid @ModelAttribute SavingGroupCreationRequest savingGroupCreationRequest
    ) {
        // AI작성: JWT 필터가 검증한 로그인 회원 번호만 계모임 개설 신청자로 사용한다.
        savingGroupCreationRequest.setMemberId(memberId);
        savingGroupCreationService.applyForCreationSavingGroup(savingGroupCreationRequest);
        return ApiResponse.ok();
    }
}
