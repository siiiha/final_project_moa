package com.nightstudy.moa.saving_group.mapper;

import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SavingGroupMapper {

    // 계모임 개설 신청 시 심사중 상태(status)에 계모임을 추가
    int applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);

    // 관리자가 계모임 개설 승인 시 계모임의 status를 승인으로 변경하고, created_at을 요청 시점으로 수정
    int approveForCreationSavingGroup(Long savingGroupId);
}
