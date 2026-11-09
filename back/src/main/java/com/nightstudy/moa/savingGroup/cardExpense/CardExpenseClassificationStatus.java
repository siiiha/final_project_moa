package com.nightstudy.moa.savingGroup.cardExpense;

// AI작성: 자동 추천 결과는 사용자 확정 전까지 통계에 반영하지 않도록 상태를 구분한다.
public enum CardExpenseClassificationStatus {
    REVIEW_PENDING,
    REVIEW_REQUIRED,
    CONFIRMED,
    CANCEL_PENDING
}
