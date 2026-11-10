package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

// AI작성: 진행 중 순번계는 조회 당시 회차와 재배정할 계원만 전송한다.
public record SavingGroupTurnOrderUpdateRequest(@NotEmpty List<@NotNull @Valid Entry> turnOrders,
        @Positive Integer currentRoundNumber) {
    public SavingGroupTurnOrderUpdateRequest(List<Entry> turnOrders) {
        this(turnOrders, null);
    }
    public record Entry(@NotNull @Positive Long memberId, @NotNull @Positive Integer sequenceNumber) {}
}
