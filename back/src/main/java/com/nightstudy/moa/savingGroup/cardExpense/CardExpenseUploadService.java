package com.nightstudy.moa.savingGroup.cardExpense;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CardExpenseUploadService {
    List<CardExpensePreview> uploadAndClassify(Long groupId, Long memberId, MultipartFile file);

    void confirmCategory(Long groupId, Long cardExpenseId, Long memberId,
                         CardExpenseClassificationConfirmRequest request);

    void confirmCategories(Long groupId, Long memberId, CardExpenseBatchConfirmationRequest request);
}
