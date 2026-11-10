package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDate;
import java.util.List;

// AI작성: 현재 유효 순번과 지정할 적용월 목록을 구분하고 지급 완료 회원은 읽기 전용으로 표시한다.
public record SavingGroupTurnOrderResponse(LocalDate effectiveMonth, int startRoundNumber,
        boolean scheduled, boolean needsReassignment, List<Member> members, Integer currentRoundNumber) {
    public record Member(String memberId, String nickname, String role, boolean paid,
            Integer currentSequenceNumber, Integer plannedSequenceNumber, String fixedReason) {}
}
