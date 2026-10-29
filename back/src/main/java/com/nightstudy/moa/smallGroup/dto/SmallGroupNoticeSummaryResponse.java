package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// AI작성: 공개 상세 화면의 최근 공지에는 목록 표시용 요약 정보만 제공한다.
@Getter
@AllArgsConstructor
public class SmallGroupNoticeSummaryResponse {
    private Long noticeId;
    private String title;
    private String writerNickname;
    private LocalDateTime createdAt;
}
