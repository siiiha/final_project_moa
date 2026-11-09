package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupTurnOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

// AI작성: 인증 회원과 검증된 본문만 Service에 전달하고 권한·저장 정책은 Service에 둔다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupTurnOrderController {
    private final SavingGroupTurnOrderService service;
    public SavingGroupTurnOrderController(SavingGroupTurnOrderService service) { this.service = service; }
    @GetMapping("/{savingGroupId}/turn-orders")
    public ApiResponse<SavingGroupTurnOrderResponse> get(
            @PathVariable("savingGroupId") @Positive Long id,
            @RequestAttribute(name = "memberId", required = false) Long user,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return ApiResponse.ok(service.get(id, user));
    }
    @PutMapping("/{savingGroupId}/turn-orders")
    public ApiResponse<SavingGroupTurnOrderResponse> update(
            @PathVariable("savingGroupId") @Positive Long id,
            @RequestAttribute(name = "memberId", required = false) Long user,
            @Valid @RequestBody SavingGroupTurnOrderUpdateRequest request) {
        return ApiResponse.ok(service.update(id, user, request));
    }
}
