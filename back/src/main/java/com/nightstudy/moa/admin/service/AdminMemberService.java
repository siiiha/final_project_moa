package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionReleaseRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionRequest;

// AI작성: 관리자 회원 목록·상세 조회 기능의 사용 규약을 정의한다.
public interface AdminMemberService {
    AdminMemberListResult getMemberList(AdminMemberSearchCondition condition);

    AdminMemberDetailResponse getMemberDetail(Long memberId);

    void restrictMember(Long administratorId, Long memberId, AdminMemberRestrictionRequest request);

    void releaseMemberRestriction(Long administratorId, Long memberId, AdminMemberRestrictionReleaseRequest request);
}
