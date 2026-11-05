package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 증빙 원본·회원 개인정보 없이 서버 판단과 확정 금액만 전달한다.
public record SavingGroupPayoutResponse(String groupType, Integer roundNumber, BigDecimal amount,
        boolean receiptRequired, Long requestId, String status) {}
