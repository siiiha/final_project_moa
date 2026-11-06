package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.*;

// AI작성: 확인값 원문은 요청에서만 사용하고 DB에는 해시만 저장한다.
public record SavingGroupDepositConfirmRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,64}") String requestKey,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String confirmationToken) {}
