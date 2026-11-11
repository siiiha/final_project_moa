package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 검증용 데이터와 공개 응답을 별도 타입으로 관리한다.
public record SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
        BigDecimal balanceAfter, String description, String purposeType, String sourceProvider, String finalCategory) {
    // AI작성: 기존 장부 행 생성 방식에서도 거래 목적과 제공자는 선택 항목이다.
    @org.apache.ibatis.annotations.AutomapConstructor
    public SavingGroupTransactionRow {}

    public SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, String description) {
        this(transactedAt, transactionType, amount, balanceAfter, description, null, null, null);
    }

    // AI작성: 기존 단위 테스트와 호출부의 7개 인자 생성 방식을 유지한다.
    public SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount,
            BigDecimal balanceAfter, String description, String purposeType, String sourceProvider) {
        this(transactedAt, transactionType, amount, balanceAfter, description, purposeType, sourceProvider, null);
    }
}
