package com.nightstudy.moa.savingGroup.cardExpense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 목록 전체의 현재 선택값을 한 번의 트랜잭션으로 저장한다.
@Getter
@Setter
@NoArgsConstructor
public class CardExpenseBatchConfirmationRequest {
    @NotEmpty(message = "확정할 카드 지출 내역이 없습니다.")
    @Valid
    private List<CardExpenseBatchConfirmationItem> items;
}
