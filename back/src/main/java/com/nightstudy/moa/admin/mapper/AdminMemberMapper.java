package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminMemberFeatureRestrictionResponse;
import com.nightstudy.moa.admin.dto.MemberRestrictionAction;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 회원 목록·상세 조회 SQL을 연결한다.
public interface AdminMemberMapper {
    // AI작성: 관리자 회원 검색 조건에 해당하는 전체 회원 수를 조회한다.
    int selectMemberCount(AdminMemberSearchCondition condition);

    // AI작성: 계산된 LIMIT과 OFFSET으로 관리자 회원 목록을 조회한다.
    List<AdminMemberListItemResponse> selectMemberList(AdminMemberSearchCondition condition);

    // AI작성: 지정한 회원의 마스킹 상세 정보와 활동 집계를 조회한다.
    AdminMemberDetailResponse selectMemberDetail(@Param("memberId") Long memberId);

    // AI작성: 회원 상세에 표시할 현재 기능별 제한을 범위 순서로 조회한다.
    List<AdminMemberFeatureRestrictionResponse> selectMemberFeatureRestrictions(
            @Param("memberId") Long memberId
    );

    // AI작성: 직접 제한·해제 중 상태가 바뀌지 않도록 대상 회원 행을 잠가 조회한다.
    AdminMemberRestrictionTarget selectRestrictionTargetForUpdate(
            @Param("memberId") Long memberId,
            @Param("restrictionScope") MemberRestrictionScope restrictionScope
    );

    // AI작성: 선택한 기능의 현재 제한을 새로 저장하거나 계산된 종료 시각으로 갱신한다.
    void upsertMemberRestriction(
            @Param("memberId") Long memberId,
            @Param("restrictionScope") MemberRestrictionScope restrictionScope,
            @Param("restrictedUntil") LocalDateTime restrictedUntil
    );

    // AI작성: 회원의 다른 기능 제한은 유지하고 선택한 기능의 현재 제한만 삭제한다.
    int releaseMemberRestriction(
            @Param("memberId") Long memberId,
            @Param("restrictionScope") MemberRestrictionScope restrictionScope
    );

    // AI작성: 스케줄러가 모든 기능 범위에서 종료 시각이 지난 기간 제한을 정리한다.
    int deleteExpiredMemberRestrictions();

    // AI작성: 직접 제한 또는 해제의 유형·사유·종료 시각을 이력 테이블에 저장한다.
    int insertRestrictionHistory(
            @Param("memberId") Long memberId,
            @Param("action") MemberRestrictionAction action,
            @Param("restrictionType") String restrictionType,
            @Param("restrictionScope") MemberRestrictionScope restrictionScope,
            @Param("reason") String reason,
            @Param("restrictedUntil") LocalDateTime restrictedUntil
    );
}
