package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminReportListResult {
    // 현재 페이지에 표시할 신고 사건 목록
    private final List<AdminReportListItemResponse> report;
    // 페이지 번호, 크기, 전체 개수, 전체 페이지 수
    private final PageMeta pageMeta;
}
