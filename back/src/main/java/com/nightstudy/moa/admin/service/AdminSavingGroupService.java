package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSavingGroupSearchCondition;

// AI작성: 관리자 계모임 목록·상세 조회 기능의 사용 규약을 정의한다.
public interface AdminSavingGroupService {
    AdminSavingGroupListResult getSavingGroupList(AdminSavingGroupSearchCondition condition);

    AdminSavingGroupDetailResponse getSavingGroupDetail(Long savingGroupId);
}
