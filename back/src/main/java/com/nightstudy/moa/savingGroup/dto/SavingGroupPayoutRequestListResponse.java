package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 기존 목록과 동일한 data 배열·meta 구조로 Controller에 전달한다.
@Getter
@AllArgsConstructor
public class SavingGroupPayoutRequestListResponse {
    private final List<SavingGroupPayoutRequestResponse> items;
    private final PageMeta pageMeta;
}
