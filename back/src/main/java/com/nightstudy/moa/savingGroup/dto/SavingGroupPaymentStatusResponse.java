package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 기준 회비가 확인되지 않으면 납부 여부와 부족액은 null이다.
public record SavingGroupPaymentStatusResponse(String memberName, BigDecimal duesAmount,
        BigDecimal depositedAmount, BigDecimal remainingAmount, Boolean paid) {}
