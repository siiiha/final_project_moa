package com.nightstudy.moa.smallGroup.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 로그인 회원의 참여 여부와 선택 보기를 상세 응답에 한정해 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupVoteMyParticipationResponse {
    private final boolean participated;
    private final List<Long> selectedOptionIds;
}
