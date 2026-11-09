package com.nightstudy.moa.savingGroup.cardExpense;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CardExpenseUploadService {
    List<CardExpensePreview> uploadAndClassify(Long groupId, Long memberId, MultipartFile file);

    // AI작성: 카드 지출에 계좌 출금을 연결한다.
    void linkTransaction(Long groupId, Long cardExpenseId, Long transactionId, Long memberId);

    void confirmCategory(Long groupId, Long cardExpenseId, Long memberId,
                         CardExpenseClassificationConfirmRequest request);

    void confirmCategories(Long groupId, Long memberId, CardExpenseBatchConfirmationRequest request);
}
