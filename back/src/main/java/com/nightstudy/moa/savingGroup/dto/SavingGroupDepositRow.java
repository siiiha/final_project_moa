package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

// AI작성: DB 담당자에게 전달한 보조 테이블과 원장 저장을 위한 내부 모델이다.
@Getter @Setter
public class SavingGroupDepositRow {
    private Long depositRequestId;
    private Long accountId;
    private Long memberId;
    private BigDecimal amount;
    // AI작성: 이번 거래 직후의 내부 장부 잔액을 기존 원장에 함께 저장한다.
    private BigDecimal balanceAfter;
    private String requestKey;
    private String status;
    private Long windowAnchorRequestId;
    private String confirmationTokenHash;
    private LocalDateTime confirmationExpiresAt;
    private Long transactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
}
