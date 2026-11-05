package com.nightstudy.moa.savingGroup.cardExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// AI작성: 엑셀 업로드 직후 사용자가 검토할 한 건의 자동 분류 미리보기다.
@Getter
@RequiredArgsConstructor
public class CardExpensePreview {
    private final LocalDate usedDate;
    private final String merchantName;
    private final BigDecimal amount;
    private final CardExpenseApprovalStatus approvalStatus;
    private final CardExpenseCategory suggestedCategory;
    private final Double confidence;
    private final CardExpenseClassificationStatus classificationStatus;
    private final CardExpenseClassificationSource classificationSource;
    private final String reviewReason;
}
