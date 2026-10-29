package com.nightstudy.moa.smallGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 소모임 투표 생성 성공 후 클라이언트가 식별할 수 있는 투표 번호만 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupVoteCreateResponse {
    private final Long smallGroupVoteId;
}
