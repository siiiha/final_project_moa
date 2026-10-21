package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);
    SavingGroupSettingsResponse getSavingGroupSettings(Long savingGroupId, Long loginMemberId);
    void updateSavingGroupSettings(Long savingGroupId, Long loginMemberId, SavingGroupSettingsPatchRequest request);
}
