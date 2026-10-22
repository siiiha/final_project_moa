package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공개 상세에는 공지 식별자와 제목만 반환한다.
@Getter
@AllArgsConstructor
public class SavingGroupNoticeSummaryResponse {
    private final Long noticeId;
    private final String title;
}
