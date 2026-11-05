package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.service.identity.AccountRecoveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth/account-recovery")
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    public AccountRecoveryController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/start")
    public ApiResponse<AccountRecoveryService.StartResponse> start() {
        return ApiResponse.ok(accountRecoveryService.start());
    }

    @PostMapping("/complete")
    public ApiResponse<List<AccountRecoveryService.FoundAccount>> complete(
            @Valid @RequestBody CompleteRequest request
    ) {
        return ApiResponse.ok(accountRecoveryService.complete(
                request.identityVerificationId(),
                request.requestToken()
        ));
    }

    public record CompleteRequest(
            @NotBlank @Size(max = 128) String identityVerificationId,
            @NotBlank @Size(max = 64) String requestToken
    ) {}
}