package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 저장된 순번과 구별되는 모의 추천 값만 반환한다.
@Getter
@AllArgsConstructor
public class SavingGroupTurnOrderRecommendationResponse {
    private final String memberId;
    private final String nickname;
    private final int recommendedTurnOrder;
    private final BigDecimal expectedPayoutAmount;
}
