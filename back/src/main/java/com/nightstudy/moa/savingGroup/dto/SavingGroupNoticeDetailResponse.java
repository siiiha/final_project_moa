package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 공지 상세 응답은 기존 공지 데이터의 식별자·본문·작성자·작성일만 반환한다.
@Getter
@AllArgsConstructor
public class SavingGroupNoticeDetailResponse {
    private final Long noticeId;
    private final String title;
    private final String content;
    private final Long writerId;
    private final String writerNickname;
    private final LocalDateTime createdAt;
}
