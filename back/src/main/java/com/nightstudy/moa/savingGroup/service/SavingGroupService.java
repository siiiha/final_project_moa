package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    // AI작성: memberId는 인증 계층에서 검증된 ID 또는 비로그인의 null만 받는다.
    SavingGroupDetailResponse getSavingGroupDetail(Long groupId, Long memberId);
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);
}
