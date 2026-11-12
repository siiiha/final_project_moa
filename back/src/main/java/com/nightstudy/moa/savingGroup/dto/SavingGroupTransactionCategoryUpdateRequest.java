package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

// AI작성: 목록 상단의 한 번 저장을 위해 변경한 거래와 선택 분류만 묶어 검증한다.
public record SavingGroupTransactionCategoryUpdateRequest(
        @NotEmpty(message = "변경할 거래를 하나 이상 선택해 주세요.") List<@NotNull @Valid Item> items
) {
    public record Item(
            @NotNull(message = "거래 ID는 필수입니다.") @Positive(message = "거래 ID는 1 이상이어야 합니다.") Long transactionId,
            @NotNull(message = "지출 분류는 필수입니다.") CardExpenseCategory category
    ) {}
}
