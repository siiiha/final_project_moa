package com.nightstudy.moa.savingGroup.cardExpense;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

// AI작성: 업로드 묶음을 먼저 저장해 카드 지출 행이 어느 업로드에서 왔는지 연결한다.
@Getter
@Setter
@RequiredArgsConstructor
public class CardExpenseUploadInsert {
    private Long uploadId;
    private final Long groupId;
    private final Long uploadedBy;
}
