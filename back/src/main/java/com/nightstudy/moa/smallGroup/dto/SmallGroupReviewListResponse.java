package com.nightstudy.moa.smallGroup.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 기존 소모임 목록처럼 후기 목록과 공통 페이징 정보를 Service에서 Controller로 전달한다.
@Getter
@AllArgsConstructor
public class SmallGroupReviewListResponse {
    private final List<SmallGroupReviewListItemResponse> items;
    private final PageMeta pageMeta;
}
