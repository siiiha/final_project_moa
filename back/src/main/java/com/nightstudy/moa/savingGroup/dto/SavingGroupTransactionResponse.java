package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 검증용 데이터와 공개 응답을 별도 타입으로 관리한다.
public record SavingGroupTransactionResponse(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
        BigDecimal balanceAfter, String purposeType, String category, String classificationStatus) {
    // AI작성: 분류 정보 없이 응답을 생성하는 기존 호출을 유지한다.
    public SavingGroupTransactionResponse(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter) {
        this(transactedAt, transactionType, amount, balanceAfter, null, null, null);
    }
}
