package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis가 투표 보기의 표시 순서와 집계된 득표 수를 함께 전달하는 내부 조회 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteOptionDetailRow {
    private Long smallGroupVoteOptionId;
    private String content;
    private int displayOrder;
    private long voteCount;
}
