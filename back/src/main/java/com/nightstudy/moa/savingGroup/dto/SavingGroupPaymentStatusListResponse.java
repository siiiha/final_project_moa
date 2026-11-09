package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;

// AI작성: 거래내역과 동일한 페이지 메타데이터를 반환한다.
public record SavingGroupPaymentStatusListResponse(List<SavingGroupPaymentStatusResponse> items, PageMeta pageMeta) {}
