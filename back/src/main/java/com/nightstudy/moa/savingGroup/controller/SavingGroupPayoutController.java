package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupPayoutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 기존 계모임 경로·JWT 속성·응답 형식을 사용하며 권한과 저장 판단은 Service에 위임한다.
@RestController
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class SavingGroupPayoutController {
    private final SavingGroupPayoutService service;
    public SavingGroupPayoutController(SavingGroupPayoutService service) { this.service = service; }

    @GetMapping("/{groupId}/payout-requests/options")
    public ResponseEntity<ApiResponse<SavingGroupPayoutResponse>> options(
            @PathVariable("groupId") @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(service.getPayoutOptions(groupId, memberId)));
    }
    // AI작성: 순번계는 빈 multipart, 친목계는 amount·title·receipt만 받는다. 계좌·거래 정보는 받지 않는다.
    @PostMapping(value = "/{groupId}/payout-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SavingGroupPayoutResponse> create(
            @PathVariable("groupId") @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @ModelAttribute SavingGroupPayoutRequest request,
            @RequestParam(name = "receipt", required = false) MultipartFile receipt) {
        return ApiResponse.ok(service.createPayout(groupId, memberId, request, receipt));
    }
    // AI작성: 공개 파일 경로를 노출하지 않고 OWNER만 원본을 내려받도록 Service에서 검증한다.
    @GetMapping("/{groupId}/payout-requests/expenses/{expenseId}/receipt")
    public ResponseEntity<Resource> receipt(
            @PathVariable("groupId") @Positive Long groupId,
            @PathVariable("expenseId") @Positive Long expenseId,
            @RequestAttribute(name = "memberId", required = false) Long memberId) {
        Resource resource = service.getReceipt(groupId, expenseId, memberId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt" + (resource.getFilename().endsWith(".png") ? ".png" : ".jpg") + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }
}
