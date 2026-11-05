package com.nightstudy.moa.savingGroup.cardExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// AI작성: 카드사 원본 열에서 분류 기능에 필요한 값만 추린 내부 파싱 결과다.
@Getter
@RequiredArgsConstructor
public class CardExpenseParsedRow {
    private final LocalDate usedDate;
    private final String merchantName;
    private final BigDecimal amount;
    private final CardExpenseApprovalStatus approvalStatus;
}
