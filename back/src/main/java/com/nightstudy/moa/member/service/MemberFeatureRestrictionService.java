package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;

// AI작성: 회원의 기능별 이용 제한을 검사하는 공통 규약이다.
public interface MemberFeatureRestrictionService {

    // AI작성: 해당 기능을 이용할 수 없으면 공통 403 예외를 발생시킨다.
    void validateAllowed(
            Long memberId,
            MemberRestrictionScope restrictionScope
    );
}