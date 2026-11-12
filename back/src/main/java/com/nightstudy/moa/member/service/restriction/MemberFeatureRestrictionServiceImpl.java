package com.nightstudy.moa.member.service.restriction;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.mapper.MemberFeatureRestrictionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// AI작성: 기능별 활성 제한을 조회하고 제한 회원의 변경 작업을 공통으로 차단한다.
public class MemberFeatureRestrictionServiceImpl
        implements MemberFeatureRestrictionService {

    private final MemberFeatureRestrictionMapper restrictionMapper;

    public MemberFeatureRestrictionServiceImpl(
            MemberFeatureRestrictionMapper restrictionMapper
    ) {
        this.restrictionMapper = restrictionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAllowed(
            Long memberId,
            MemberRestrictionScope restrictionScope
    ) {
        if (isRestricted(memberId, restrictionScope)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FEATURE_RESTRICTED",
                    "해당 기능의 이용이 제한되었습니다."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
// AI작성: 영구 제한 또는 만료되지 않은 기능 제한 여부를 조회한다.
    public boolean isRestricted(
            Long memberId,
            MemberRestrictionScope restrictionScope
    ) {
        return restrictionMapper.existsActiveRestriction(
                memberId,
                restrictionScope
        );
    }

}