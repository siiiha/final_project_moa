package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 입금 집계 결과에는 내부 거래 식별자를 포함하지 않는다.
public record SavingGroupPaymentStatusRow(String memberName, BigDecimal depositedAmount) {}
