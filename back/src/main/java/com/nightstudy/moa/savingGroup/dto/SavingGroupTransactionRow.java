package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 검증용 데이터와 공개 응답을 별도 타입으로 관리한다.
public record SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
        BigDecimal balanceAfter, String description, String purposeType, String category, String classificationStatus) {
    // AI작성: 기존 장부 행 생성 방식에서도 분류 정보는 선택 항목이다.
    @org.apache.ibatis.annotations.AutomapConstructor
    public SavingGroupTransactionRow {}

    public SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, String description) {
        this(transactedAt, transactionType, amount, balanceAfter, description, null, null, null);
    }
}
