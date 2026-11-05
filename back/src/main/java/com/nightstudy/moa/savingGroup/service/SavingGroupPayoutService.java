package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupPayoutResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 계금 요청·요청 가능 정보·OWNER 전용 증빙 조회 계약이며 지급 처리는 포함하지 않는다.
public interface SavingGroupPayoutService {
    SavingGroupPayoutResponse getPayoutOptions(Long groupId, Long memberId);
    SavingGroupPayoutResponse createPayout(Long groupId, Long memberId, SavingGroupPayoutRequest request, MultipartFile receipt);
    Resource getReceipt(Long groupId, Long expenseId, Long memberId);
}
