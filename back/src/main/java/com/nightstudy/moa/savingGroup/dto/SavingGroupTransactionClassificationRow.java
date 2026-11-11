package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 자동 분류 원본 결과를 사용자 최종 분류와 분리해 저장·재사용한다.
public record SavingGroupTransactionClassificationRow(
        Long transactionId,
        String suggestedCategory,
        String classificationStatus,
        BigDecimal confidence,
        String predictionSource,
        String reviewReason,
        String modelVersion
) {
    @org.apache.ibatis.annotations.AutomapConstructor
    public SavingGroupTransactionClassificationRow {}
}
