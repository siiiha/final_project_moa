package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupDepositService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

// AI작성: 기존 인증 속성·별칭·응답을 사용하며 모든 입금 판단은 Service에 위임한다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupDepositController {
    private final SavingGroupDepositService service;
    public SavingGroupDepositController(SavingGroupDepositService service) { this.service = service; }
    @PostMapping("/{savingGroupId}/account/deposits")
    public ApiResponse<SavingGroupDepositResponse> deposit(
            @PathVariable("savingGroupId") @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupDepositRequest request) {
        return ApiResponse.ok(service.deposit(groupId, memberId, request));
    }
    @PostMapping("/{savingGroupId}/account/deposits/confirm")
    public ApiResponse<SavingGroupDepositResponse> confirm(
            @PathVariable("savingGroupId") @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody SavingGroupDepositConfirmRequest request) {
        return ApiResponse.ok(service.confirm(groupId, memberId, request));
    }
}
