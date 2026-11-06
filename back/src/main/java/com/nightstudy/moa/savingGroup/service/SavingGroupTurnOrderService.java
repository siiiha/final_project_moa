package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;

// AI작성: 순번 지정 정책을 기존 추천 서비스와 분리한다.
public interface SavingGroupTurnOrderService {
    SavingGroupTurnOrderResponse get(Long savingGroupId, Long memberId);
    SavingGroupTurnOrderResponse update(Long savingGroupId, Long memberId, SavingGroupTurnOrderUpdateRequest request);
}
