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

    int confirmCardExpenseCategory(
            @Param("cardExpenseId") Long cardExpenseId,
            @Param("finalCategory") CardExpenseCategory finalCategory,
            @Param("confirmedBy") Long confirmedBy,
            @Param("confirmedAt") LocalDateTime confirmedAt
    );
}
