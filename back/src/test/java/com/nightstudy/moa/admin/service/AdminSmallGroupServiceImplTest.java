package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.admin.dto.AdminGroupRestrictionRequest;
import com.nightstudy.moa.admin.dto.AdminSmallGroupRestrictionTarget;
import com.nightstudy.moa.admin.dto.AdminSmallGroupStatus;
import com.nightstudy.moa.admin.dto.RestrictionAction;
import com.nightstudy.moa.admin.mapper.AdminSmallGroupMapper;
import com.nightstudy.moa.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 소모임 운영정지·해제의 상태 전환과 실패 분기를 DB 없이 검증한다.
class AdminSmallGroupServiceImplTest {
    @Mock
    private AdminSmallGroupMapper adminSmallGroupMapper;

    @InjectMocks
    private AdminSmallGroupServiceImpl adminSmallGroupService;

    @Test
    void restrictSmallGroupChangesStatusAndStoresHistory() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("ACTIVE"));
        when(adminSmallGroupMapper.updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.ACTIVE, AdminSmallGroupStatus.RESTRICTED))
                .thenReturn(1);
        when(adminSmallGroupMapper.insertRestrictionHistory(
                1L, RestrictionAction.RESTRICT, "운영 정책 위반"))
                .thenReturn(1);

        adminSmallGroupService.restrictSmallGroup(
                1L, restrictionRequest("운영 정책 위반"));

        verify(adminSmallGroupMapper).updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.ACTIVE, AdminSmallGroupStatus.RESTRICTED);
        verify(adminSmallGroupMapper).insertRestrictionHistory(
                1L, RestrictionAction.RESTRICT, "운영 정책 위반");
    }

    @Test
    void releaseSmallGroupChangesStatusAndStoresHistory() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("RESTRICTED"));
        when(adminSmallGroupMapper.selectLatestRestrictionAction(1L)).thenReturn("RESTRICT");
        when(adminSmallGroupMapper.updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.RESTRICTED, AdminSmallGroupStatus.ACTIVE))
                .thenReturn(1);
        when(adminSmallGroupMapper.insertRestrictionHistory(
                1L, RestrictionAction.RELEASE, "재검토 완료"))
                .thenReturn(1);

        adminSmallGroupService.releaseSmallGroupRestriction(
                1L, restrictionRequest("재검토 완료"));

        verify(adminSmallGroupMapper).updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.RESTRICTED, AdminSmallGroupStatus.ACTIVE);
        verify(adminSmallGroupMapper).insertRestrictionHistory(
                1L, RestrictionAction.RELEASE, "재검토 완료");
    }

    @Test
    void restrictSmallGroupThrowsWhenTargetDoesNotExist() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(999L)).thenReturn(null);

        assertThatThrownBy(() -> adminSmallGroupService.restrictSmallGroup(
                999L, restrictionRequest("운영 정책 위반")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
                });
    }

    @Test
    void restrictSmallGroupRejectsAlreadyRestrictedTarget() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("RESTRICTED"));

        assertThatThrownBy(() -> adminSmallGroupService.restrictSmallGroup(
                1L, restrictionRequest("운영 정책 위반")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("SMALL_GROUP_RESTRICTION_UNAVAILABLE"));
        verify(adminSmallGroupMapper, never()).updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.ACTIVE, AdminSmallGroupStatus.RESTRICTED);
    }

    @Test
    void releaseSmallGroupRejectsTargetWithoutActiveRestrictionHistory() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("RESTRICTED"));
        when(adminSmallGroupMapper.selectLatestRestrictionAction(1L)).thenReturn("RELEASE");

        assertThatThrownBy(() -> adminSmallGroupService.releaseSmallGroupRestriction(
                1L, restrictionRequest("재검토 완료")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("SMALL_GROUP_NOT_ADMIN_RESTRICTED"));
        verify(adminSmallGroupMapper, never()).updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.RESTRICTED, AdminSmallGroupStatus.ACTIVE);
    }

    @Test
    void restrictSmallGroupDoesNotStoreHistoryWhenStatusChangeFails() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("ACTIVE"));
        when(adminSmallGroupMapper.updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.ACTIVE, AdminSmallGroupStatus.RESTRICTED))
                .thenReturn(0);

        assertThatThrownBy(() -> adminSmallGroupService.restrictSmallGroup(
                1L, restrictionRequest("운영 정책 위반")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_RESTRICTION_FAILED"));
        verify(adminSmallGroupMapper, never()).insertRestrictionHistory(
                1L, RestrictionAction.RESTRICT, "운영 정책 위반");
    }

    @Test
    void restrictSmallGroupThrowsWhenHistoryStorageFails() {
        when(adminSmallGroupMapper.selectRestrictionTargetForUpdate(1L))
                .thenReturn(restrictionTarget("ACTIVE"));
        when(adminSmallGroupMapper.updateSmallGroupStatus(
                1L, AdminSmallGroupStatus.ACTIVE, AdminSmallGroupStatus.RESTRICTED))
                .thenReturn(1);
        when(adminSmallGroupMapper.insertRestrictionHistory(
                1L, RestrictionAction.RESTRICT, "운영 정책 위반"))
                .thenReturn(0);

        assertThatThrownBy(() -> adminSmallGroupService.restrictSmallGroup(
                1L, restrictionRequest("운영 정책 위반")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_RESTRICTION_FAILED"));
    }

    private AdminSmallGroupRestrictionTarget restrictionTarget(String status) {
        AdminSmallGroupRestrictionTarget target = new AdminSmallGroupRestrictionTarget();
        target.setSmallGroupId(1L);
        target.setStatus(status);
        return target;
    }

    private AdminGroupRestrictionRequest restrictionRequest(String reason) {
        AdminGroupRestrictionRequest request = new AdminGroupRestrictionRequest();
        request.setReason(reason);
        return request;
    }
}
