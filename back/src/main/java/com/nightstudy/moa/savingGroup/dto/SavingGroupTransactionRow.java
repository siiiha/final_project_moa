package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI작성: 내부 검증용 데이터와 공개 응답을 별도 타입으로 관리한다.
public record SavingGroupTransactionRow(LocalDateTime transactedAt, String transactionType, BigDecimal amount, BigDecimal balanceAfter, String description) {
}
