package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

// AI작성: 전체 미지급 승인 회원의 실제 지급 회차를 요청한다.
public record SavingGroupTurnOrderUpdateRequest(@NotEmpty List<@NotNull @Valid Entry> turnOrders) {
    public record Entry(@NotNull @Positive Long memberId, @NotNull @Positive Integer sequenceNumber) {}
}
