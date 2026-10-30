package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminGroupRestrictionRequest;

// AI작성: 관리자 소모임 운영정지와 해제 기능의 공개 계약을 정의한다.
public interface AdminSmallGroupService {
    void restrictSmallGroup(Long smallGroupId, AdminGroupRestrictionRequest request);

    void releaseSmallGroupRestriction(Long smallGroupId, AdminGroupRestrictionRequest request);
}
