package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListItemResponse;
import com.nightstudy.moa.admin.dto.AdminMemberSearchCondition;
import com.nightstudy.moa.admin.dto.AdminMemberRestrictionTarget;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 회원 목록·상세 조회 SQL을 연결한다.
public interface AdminMemberMapper {
    int selectMemberCount(AdminMemberSearchCondition condition);

    List<AdminMemberListItemResponse> selectMemberList(AdminMemberSearchCondition condition);

    AdminMemberDetailResponse selectMemberDetail(@Param("memberId") Long memberId);

    AdminMemberRestrictionTarget selectRestrictionTargetForUpdate(@Param("memberId") Long memberId);

    int updateMemberRestriction(
            @Param("memberId") Long memberId,
            @Param("suspendedUntil") LocalDateTime suspendedUntil
    );

    int releaseMemberRestriction(@Param("memberId") Long memberId);

    int insertRestrictionHistory(
            @Param("memberId") Long memberId,
            @Param("action") String action,
            @Param("restrictionType") String restrictionType,
            @Param("reason") String reason,
            @Param("restrictedUntil") LocalDateTime restrictedUntil
    );
}
