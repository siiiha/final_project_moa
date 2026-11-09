package com.nightstudy.moa.savingGroup.cardExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

// AI작성: 자동 추천 결과는 확정 전 상태로 카드 지출 행에 저장한다.
@Getter
@Setter
@RequiredArgsConstructor
public class CardExpenseInsert {
    private Long cardExpenseId;
    private final Long uploadId;
    private final LocalDate usedDate;
    private final String merchantName;
    private final BigDecimal amount;
    private final CardExpenseApprovalStatus approvalStatus;
    private final CardExpenseCategory suggestedCategory;
    private final CardExpenseClassificationStatus classificationStatus;
    private final Double confidenceScore;
    private final CardExpenseClassificationSource classificationSource;
    private final String reviewReason;
}
