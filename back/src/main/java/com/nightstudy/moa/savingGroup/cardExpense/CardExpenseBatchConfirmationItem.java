package com.nightstudy.moa.savingGroup.cardExpense;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 상단 확정하기에서 저장할 카드 지출 한 건의 최종 선택값이다.
@Getter
@Setter
@NoArgsConstructor
public class CardExpenseBatchConfirmationItem {
    @NotNull(message = "카드 지출 번호를 확인해 주세요.")
    private Long cardExpenseId;

    @NotNull(message = "최종 지출 항목을 선택해 주세요.")
    private CardExpenseCategory finalCategory;
}
