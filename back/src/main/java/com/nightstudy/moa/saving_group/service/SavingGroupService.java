package com.nightstudy.moa.saving_group.service;

import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;

public interface SavingGroupService {

    boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);

    boolean approveForCreationSavingGroup(Long savingGroupId);
}
