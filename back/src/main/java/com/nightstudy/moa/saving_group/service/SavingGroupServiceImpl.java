package com.nightstudy.moa.saving_group.service;

import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.saving_group.mapper.SavingGroupMapper;
import org.springframework.stereotype.Service;

@Service
public class SavingGroupServiceImpl implements SavingGroupService {

    private final SavingGroupMapper savingGroupMapper;

    public SavingGroupServiceImpl(SavingGroupMapper savingGroupMapper) {
        this.savingGroupMapper = savingGroupMapper;
    }

    @Override
    public void applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {

        int result = savingGroupMapper.applyForCreationSavingGroup(savingGroupCreationRequest);

        if (result != 1) {
            // TODO: 실제 DB에 심사중인 상태에 계모임으로 저장되었는지 구분 (안되어있을 경우 예외 발생)
            throw new IllegalStateException("applyForCreationSavingGroup() failed");
        }

    }

}