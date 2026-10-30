package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 공개 투표에서 보기별 참여자 닉네임을 묶기 위한 내부 조회 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteOptionVoterNicknameRow {
    private Long smallGroupVoteOptionId;
    private String voterNickname;
}
