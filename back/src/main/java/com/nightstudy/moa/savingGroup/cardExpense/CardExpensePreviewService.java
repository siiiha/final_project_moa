package com.nightstudy.moa.savingGroup.cardExpense;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CardExpensePreviewService {
    List<CardExpensePreview> createPreview(Long groupId, Long memberId, MultipartFile file);
}
