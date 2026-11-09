package com.nightstudy.moa.savingGroup.cardExpense;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 확정 요청 시 지출 행의 모임 소속·승인 상태를 잠근 상태로 다시 확인한다.
@Getter
@Setter
@NoArgsConstructor
public class CardExpenseConfirmationTarget {
    private Long cardExpenseId;
    private Long groupId;
    private CardExpenseApprovalStatus approvalStatus;
    private CardExpenseClassificationStatus classificationStatus;
}
