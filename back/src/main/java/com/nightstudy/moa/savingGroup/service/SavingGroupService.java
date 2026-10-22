package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);

    // AI작성: 인증된 회원 본인 명의로 계모임 가입 신청을 대기 상태로 등록한다.
    void requestSavingGroupJoin(Long savingGroupId, Long memberId);
}
