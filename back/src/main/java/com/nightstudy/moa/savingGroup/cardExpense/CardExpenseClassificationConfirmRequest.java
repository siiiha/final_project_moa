package com.nightstudy.moa.savingGroup.cardExpense;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 카드 지출의 최종 항목은 계주 또는 회계 담당자가 명시적으로 선택해야 확정한다.
@Getter
@Setter
@NoArgsConstructor
public class CardExpenseClassificationConfirmRequest {
    @NotNull(message = "최종 지출 항목을 선택해 주세요.")
    private CardExpenseCategory finalCategory;
}
