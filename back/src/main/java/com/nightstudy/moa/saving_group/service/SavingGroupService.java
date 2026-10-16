package com.nightstudy.moa.saving_group.service;

import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;

public interface SavingGroupService {

    void applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);

    void approveForCreationSavingGroup(Long savingGroupId);
}
