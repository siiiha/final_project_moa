package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminGroupRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminSmallGroupListResult;
import com.nightstudy.moa.admin.dto.AdminSmallGroupSearchCondition;
import com.nightstudy.moa.admin.dto.AdminSmallGroupSummaryResponse;

// AI작성: 관리자 소모임 운영정지와 해제 기능의 공개 계약을 정의한다.
public interface AdminSmallGroupService {
    AdminSmallGroupSummaryResponse getSmallGroupSummary();

    AdminSmallGroupListResult getSmallGroupList(AdminSmallGroupSearchCondition condition);

    void restrictSmallGroup(Long smallGroupId, AdminGroupRestrictionRequest request);

    void releaseSmallGroupRestriction(Long smallGroupId, AdminGroupRestrictionRequest request);
}
