package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupListResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsPatchRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupSettingsResponse;

// AI작성: 공개 계모임 목록 조회 계약이다.
public interface SavingGroupService {
    // AI작성: memberId는 인증 계층에서 검증된 ID 또는 비로그인의 null만 받는다.
    SavingGroupDetailResponse getSavingGroupDetail(Long groupId, Long memberId);
    SavingGroupListResponse getSavingGroupList(SavingGroupSearchCondition condition);
    SavingGroupSettingsResponse getSavingGroupSettings(Long savingGroupId, Long loginMemberId);
    void updateSavingGroupSettings(Long savingGroupId, Long loginMemberId, SavingGroupSettingsPatchRequest request);
    void applyDueSettingChanges();
    void changeSavingGroupStatus(Long groupId, String nextStatus);
    // AI작성: 인증된 회원 본인 명의로 계모임 가입 신청을 대기 상태로 등록한다.
    void requestSavingGroupJoin(Long savingGroupId, Long memberId);
}
