package com.nightstudy.moa.savingGroup.cardExpense;

// AI작성: 화면 표시용 한글 이름과 DB·API에서 사용할 고정 분류 값을 함께 관리한다.
public enum CardExpenseCategory {
    FOOD("식비"),
    TRANSPORTATION("교통비"),
    RENTAL("대관료"),
    SUPPLIES("물품비"),
    ACTIVITY("활동비"),
    ACCOMMODATION("숙박비"),
    FEE("수수료"),
    OTHER("기타");

    private final String displayName;

    CardExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
