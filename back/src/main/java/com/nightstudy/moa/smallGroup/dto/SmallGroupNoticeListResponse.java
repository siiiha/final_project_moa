package com.nightstudy.moa.smallGroup.dto;
import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
// AI작성: 공지 목록과 일반 공지 페이징을 함께 전달한다.
@Getter @AllArgsConstructor
public class SmallGroupNoticeListResponse {
    private final List<SmallGroupNoticeSummaryResponse> items;
    private final PageMeta pageMeta;
}
