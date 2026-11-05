package com.nightstudy.moa.savingGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 기존 상세 응답 구조를 유지하면서 Controller가 공지 페이징 정보를 공통 meta로 분리할 수 있게 한다.
@Getter
@AllArgsConstructor
public class SavingGroupDetailPageResponse {
    private final SavingGroupDetailResponse detail;
    private final PageMeta pageMeta;
}
