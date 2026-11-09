package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 거래·회원·계좌 ID는 반환하지 않고 확인값은 대기 응답에서만 반환한다.
public record SavingGroupDepositResponse(String status, BigDecimal amount, BigDecimal balance,
        String confirmationToken, LocalDateTime confirmationExpiresAt, String message) {}
