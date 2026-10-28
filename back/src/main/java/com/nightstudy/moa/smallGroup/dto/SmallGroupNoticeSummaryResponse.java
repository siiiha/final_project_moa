package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공개 상세 화면의 최근 공지에는 목록 표시용 요약 정보만 제공한다.
@Getter
@AllArgsConstructor
public class SmallGroupNoticeSummaryResponse {
    private final Long noticeId;
    private final String title;
    private final String writerNickname;
    private final LocalDateTime createdAt;
}
