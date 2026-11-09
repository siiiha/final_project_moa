package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 모의 추천 대상의 식별자와 닉네임만 조회한다.
@Getter
@Setter
public class SavingGroupTurnOrderMemberRow {
    private String memberId;
    private String nickname;
}
