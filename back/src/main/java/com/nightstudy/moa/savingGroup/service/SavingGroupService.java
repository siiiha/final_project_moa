package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);
}
