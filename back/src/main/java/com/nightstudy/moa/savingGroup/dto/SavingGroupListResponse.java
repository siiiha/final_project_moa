package com.nightstudy.moa.savingGroup.dto;

import java.util.List;
import com.nightstudy.moa.common.dto.PageMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: Service 결과를 Controller에 전달하며 공통 PageMeta를 재사용한다.
@Getter
@AllArgsConstructor
public class SavingGroupListResponse {
    private final List<SavingGroupSummaryResponse> items;
    private final PageMeta pageMeta;
}
