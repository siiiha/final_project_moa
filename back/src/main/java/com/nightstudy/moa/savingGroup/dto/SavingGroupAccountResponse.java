package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 계좌 원문·예금주·내부 주문번호 없이 승인 모임원에게만 제공한다.
public record SavingGroupAccountResponse(boolean issued, String bankCode, String maskedAccountNumber,
        BigDecimal balance, boolean testAccount) {}
