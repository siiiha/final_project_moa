package com.nightstudy.moa.savingGroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 고정 크기 페이징 정보와 카드 목록을 공통 응답의 data에 담는다.
@Getter
@AllArgsConstructor
public class SavingGroupListResponse {
    private final List<SavingGroupSummaryResponse> items;
    private final long totalElements;
    private final long totalPages;
    private final int currentPage;
    private final int pageSize;
}
