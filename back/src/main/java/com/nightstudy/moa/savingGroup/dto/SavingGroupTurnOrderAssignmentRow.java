package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 개인정보 대신 닉네임과 회원 식별자를 사용하고 지급 완료 여부를 구분한다.
@Getter @Setter
public class SavingGroupTurnOrderAssignmentRow {
    private String memberId;
    private String nickname;
    private String role;
    private Integer sequenceNumber;
    private boolean paid;
}
