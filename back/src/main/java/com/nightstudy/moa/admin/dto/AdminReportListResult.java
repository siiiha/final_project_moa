package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// AI작성: 신고 목록 데이터와 페이징 정보를 Service에서 Controller로 함께 전달한다.
public class AdminReportListResult {
    // AI작성: 현재 페이지에 표시할 신고 사건 목록이다.
    private final List<AdminReportListItemResponse> report;
    // AI작성: 페이지 번호, 크기, 전체 개수, 전체 페이지 수를 담는다.
    private final PageMeta pageMeta;
}
