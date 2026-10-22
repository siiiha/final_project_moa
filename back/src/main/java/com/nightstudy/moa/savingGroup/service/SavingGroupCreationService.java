package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;

// AI작성: 개설 신청 기능을 목록·상세 Service와 구분해 같은 도메인 패키지에서 관리한다.
public interface SavingGroupCreationService {
    boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);

    boolean approveForCreationSavingGroup(Long savingGroupId);
}
