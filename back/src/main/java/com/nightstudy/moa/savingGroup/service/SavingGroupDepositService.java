package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;

// AI작성: 금융 발급 서비스와 분리된 내부 테스트 입금 전용 계약이다.
public interface SavingGroupDepositService {
    SavingGroupDepositResponse deposit(Long groupId, Long memberId, SavingGroupDepositRequest request);
    SavingGroupDepositResponse confirm(Long groupId, Long memberId, SavingGroupDepositConfirmRequest request);
}
