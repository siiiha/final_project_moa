package com.nightstudy.moa.smallGroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 투표 보기별 득표 현황과 공개 투표에서만 노출할 참여자 닉네임 목록을 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupVoteOptionDetailResponse {
    private final Long smallGroupVoteOptionId;
    private final String content;
    private final int displayOrder;
    private final long voteCount;
    private final List<String> voterNicknames;
}
