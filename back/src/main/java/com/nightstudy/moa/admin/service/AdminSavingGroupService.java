package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSavingGroupRestrictionRequest;

// AI작성: 관리자 계모임 목록·상세 조회 기능의 사용 규약을 정의한다.
public interface AdminSavingGroupService {
    AdminSavingGroupListResult getSavingGroupList(AdminSavingGroupSearchCondition condition);

    AdminSavingGroupDetailResponse getSavingGroupDetail(Long savingGroupId);

    // AI작성: 진행 중 계모임을 관리자 제한 상태로 전환하고 사유 이력을 저장한다.
    void restrictSavingGroup(Long savingGroupId, AdminSavingGroupRestrictionRequest request);

    // AI작성: 관리자 제한 중인 계모임을 진행 상태로 복구하고 해제 사유를 저장한다.
    void releaseSavingGroupRestriction(Long savingGroupId, AdminSavingGroupRestrictionRequest request);
}
