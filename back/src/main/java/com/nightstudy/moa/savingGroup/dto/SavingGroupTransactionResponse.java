package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 검증용 데이터와 공개 응답을 별도 타입으로 관리한다.
public record SavingGroupTransactionResponse(Long transactionId, LocalDateTime transactedAt, String transactionType, BigDecimal amount,
        BigDecimal balanceAfter, String purposeType, String category, String classificationStatus, boolean categoryEditable) {
    // AI작성: 기존 거래 조회 테스트·호출부는 식별자 없이도 응답을 생성할 수 있도록 유지한다.
    public SavingGroupTransactionResponse(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, String purposeType, String category, String classificationStatus) {
        this(null, transactedAt, transactionType, amount, balanceAfter, purposeType, category, classificationStatus, false);
    }

    // AI작성: 분류 정보 없이 응답을 생성하는 기존 호출을 유지한다.
    public SavingGroupTransactionResponse(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter) {
        this(null, transactedAt, transactionType, amount, balanceAfter, null, null, null, false);
    }
}
