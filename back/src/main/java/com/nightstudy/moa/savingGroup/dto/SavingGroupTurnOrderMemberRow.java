package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 승인 계원의 추천 표시 정보와 현재 순번 및 지급 완료 여부를 조회한다.
@Getter
@Setter
public class SavingGroupTurnOrderMemberRow {
    private String memberId;
    private String nickname;
    private Integer sequenceNumber;
    private boolean paid;
}
