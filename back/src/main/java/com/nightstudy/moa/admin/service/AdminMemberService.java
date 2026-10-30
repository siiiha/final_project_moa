package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionReleaseRequest;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionRequest;

// AI작성: 관리자 회원 목록·상세 조회 기능의 사용 규약을 정의한다.
public interface AdminMemberService {
    // AI작성: 관리자 회원 검색 조건에 맞는 목록과 페이징 정보를 조회한다.
    AdminMemberListResult getMemberList(AdminMemberSearchCondition condition);

    // AI작성: 지정한 회원의 관리자용 상세 정보를 조회한다.
    AdminMemberDetailResponse getMemberDetail(Long memberId);

    // AI작성: 인증된 관리자가 대상 회원에게 기간제 또는 영구 제한을 적용한다.
    void restrictMember(Long administratorId, Long memberId, AdminMemberRestrictionRequest request);

    // AI작성: 인증된 관리자가 대상 회원의 현재 이용 제한을 해제한다.
    void releaseMemberRestriction(Long administratorId, Long memberId, AdminMemberRestrictionReleaseRequest request);
}
