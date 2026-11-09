package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

// AI작성: 공개 상세의 공지 목록에 식별자·제목·작성자 닉네임·작성일을 반환한다.
@Getter
@AllArgsConstructor
public class SavingGroupNoticeSummaryResponse {
    private final Long noticeId;
    private final String title;
    private final String writerNickname;
    private final LocalDateTime createdAt;
}
