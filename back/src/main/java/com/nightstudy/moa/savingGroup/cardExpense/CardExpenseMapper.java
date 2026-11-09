package com.nightstudy.moa.savingGroup.cardExpense;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CardExpenseMapper {
    int insertCardExpenseUpload(CardExpenseUploadInsert upload);

    int insertCardExpense(CardExpenseInsert expense);

    CardExpenseConfirmationTarget selectCardExpenseConfirmationTargetForUpdate(
            @Param("cardExpenseId") Long cardExpenseId
    );

    // AI작성: 같은 모임의 출금을 카드 지출에 연결한다.
    Long selectWithdrawal(@Param("groupId") Long groupId, @Param("transactionId") Long transactionId);

    int linkTransaction(@Param("cardExpenseId") Long cardExpenseId, @Param("transactionId") Long transactionId);

    int confirmCardExpenseCategory(
            @Param("cardExpenseId") Long cardExpenseId,
            @Param("finalCategory") CardExpenseCategory finalCategory,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedAt") LocalDateTime confirmedAt
    );
}
