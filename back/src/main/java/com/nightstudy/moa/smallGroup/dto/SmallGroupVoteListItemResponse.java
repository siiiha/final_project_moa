package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 소모임 투표 목록에는 제목과 등록·마감 일시만 노출하는 최소 응답 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteListItemResponse {
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime endsAt;
}
