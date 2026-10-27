package com.nightstudy.moa.smallGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 목록 데이터와 공통 페이징 정보를 Controller에 함께 전달한다.
@Getter
@AllArgsConstructor
public class SmallGroupListResponse {
    private final List<SmallGroupSummaryResponse> items;
    private final PageMeta pageMeta;
}
