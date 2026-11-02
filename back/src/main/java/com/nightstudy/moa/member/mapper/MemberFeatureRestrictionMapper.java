package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 일반 회원 기능에서 현재 적용 중인 영역별 제한 여부를 조회한다.
public interface MemberFeatureRestrictionMapper {

    // AI작성: 영구 제한 또는 아직 만료되지 않은 기간 제한이 존재하는지 확인한다.
    boolean existsActiveRestriction(
            @Param("memberId") Long memberId,
            @Param("restrictionScope") MemberRestrictionScope restrictionScope
    );
}