package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 목록 행이 투표 상세로 이동할 수 있도록 식별자와 제목·진행 상태·마감 시각을 노출한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteListItemResponse {
    private Long smallGroupVoteId;
    private String title;
    private String status;
    private LocalDateTime endsAt;
}
