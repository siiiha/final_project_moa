package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;

// AI작성: 거래 목록과 기존 공통 페이지 정보를 함께 전달한다.
public record SavingGroupTransactionListResponse(List<SavingGroupTransactionResponse> items, PageMeta pageMeta) {
}
