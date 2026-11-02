package com.nightstudy.moa.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import com.nightstudy.moa.member.mapper.MemberFeatureRestrictionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 기능 제한 존재 여부에 따라 공통 검사가 허용 또는 403 예외를 반환하는지 확인한다.
class MemberFeatureRestrictionServiceImplTest {

    @Mock
    private MemberFeatureRestrictionMapper restrictionMapper;

    @InjectMocks
    private MemberFeatureRestrictionServiceImpl restrictionService;

    @Test
    void validateAllowedPassesWhenRestrictionDoesNotExist() {
        when(restrictionMapper.existsActiveRestriction(
                1L,
                MemberRestrictionScope.CONTENT
        )).thenReturn(false);

        assertThatCode(() -> restrictionService.validateAllowed(
                1L,
                MemberRestrictionScope.CONTENT
        )).doesNotThrowAnyException();
    }

    @Test
    void validateAllowedThrowsForbiddenWhenRestrictionExists() {
        when(restrictionMapper.existsActiveRestriction(
                1L,
                MemberRestrictionScope.CONTENT
        )).thenReturn(true);

        assertThatThrownBy(() -> restrictionService.validateAllowed(
                1L,
                MemberRestrictionScope.CONTENT
        )).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.getStatus().value()).isEqualTo(403);
            assertThat(exception.getCode()).isEqualTo("FEATURE_RESTRICTED");
        });
    }
}
