package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: DB 대역으로 권한·상태·두 개 제한·잠금 순서·동일 상태 재요청 및 고정 목록 반환을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticePinServiceTest {
    @Mock private SavingGroupMapper mapper;
    @InjectMocks private SavingGroupServiceImpl service;

    @ParameterizedTest
    @CsvSource({"OWNER,01", "MANAGER,01", "OWNER,02", "MANAGER,02"})
    void approvedOperatorsCanPinWithSerializedLockOrder(String role, String status) {
        target(status); relation(role, "APPROVED"); notice(false);
        when(mapper.selectSavingGroupPinnedNoticeIdsForUpdate(20L)).thenReturn(List.of(41L));
        when(mapper.updateSavingGroupNoticePinned(20L, 40L, true)).thenReturn(1);
        change(true);
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupNoticeTargetForUpdate(20L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
        order.verify(mapper).selectSavingGroupNoticePinForUpdate(20L, 40L);
        order.verify(mapper).selectSavingGroupPinnedNoticeIdsForUpdate(20L);
        order.verify(mapper).updateSavingGroupNoticePinned(20L, 40L, true);
        verifyNoMoreInteractions(mapper);
    }

    @Test void rejectsThirdPin() {
        target("01"); relation("OWNER", "APPROVED"); notice(false);
        when(mapper.selectSavingGroupPinnedNoticeIdsForUpdate(20L)).thenReturn(List.of(41L, 42L));
        failure(() -> change(true), 409, "SAVING_GROUP_NOTICE_PIN_LIMIT_EXCEEDED");
        verify(mapper, never()).updateSavingGroupNoticePinned(any(), any(), anyBoolean());
    }

    @Test void unpinDoesNotRequireFreeSlot() {
        target("01"); relation("MANAGER", "APPROVED"); notice(true);
        when(mapper.updateSavingGroupNoticePinned(20L, 40L, false)).thenReturn(1);
        change(false);
        verify(mapper, never()).selectSavingGroupPinnedNoticeIdsForUpdate(any());
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void repeatedStateIsSuccessfulWithoutUpdate(boolean pinned) {
        target("01"); relation("OWNER", "APPROVED"); notice(pinned);
        change(pinned);
        verify(mapper, never()).updateSavingGroupNoticePinned(any(), any(), anyBoolean());
        verify(mapper, never()).selectSavingGroupPinnedNoticeIdsForUpdate(any());
    }

    @ParameterizedTest @NullSource @ValueSource(longs = {0, -1})
    void requiresLogin(Long memberId) {
        failure(() -> service.updateSavingGroupNoticePinned(20L, 40L, memberId, true), 401, "UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @CsvSource({",40,true", "0,40,true", "-1,40,true", "20,,true", "20,0,true", "20,-1,true", "20,40,"})
    void rejectsInvalidInput(Long groupId, Long noticeId, Boolean pinned) {
        failure(() -> service.updateSavingGroupNoticePinned(groupId, noticeId, 30L, pinned), 400, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @Test void missingGroupStopsLookup() {
        failure(() -> change(true), 404, "NOT_FOUND");
        verify(mapper).selectSavingGroupNoticeTargetForUpdate(20L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"00", "03", "04", "UNKNOWN"})
    void rejectsInactiveGroup(String status) {
        target(status);
        failure(() -> change(true), 409, "SAVING_GROUP_ACTIVITY_RESTRICTED");
        verify(mapper, never()).selectSavingGroupMembershipForUpdate(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"MEMBER,APPROVED", "OWNER,PENDING", "MANAGER,REJECTED", "OWNER,LEFT", "MANAGER,EXPELLED", ",APPROVED", "UNKNOWN,APPROVED"})
    void rejectsOtherRolesAndUnapprovedMembership(String role, String status) {
        target("01"); relation(role, status);
        failure(() -> change(true), 403, "SAVING_GROUP_NOTICE_PIN_FORBIDDEN");
        verify(mapper, never()).selectSavingGroupNoticePinForUpdate(any(), any());
    }

    @Test void rejectsMissingMembership() {
        target("01");
        failure(() -> change(true), 403, "SAVING_GROUP_NOTICE_PIN_FORBIDDEN");
    }

    @Test void rejectsMissingOrOtherGroupsNotice() {
        target("01"); relation("OWNER", "APPROVED");
        failure(() -> change(true), 404, "SAVING_GROUP_NOTICE_NOT_FOUND");
    }

    @Test void rejectsUnexpectedUpdateCount() {
        target("01"); relation("OWNER", "APPROVED"); notice(false);
        failure(() -> change(true), 500, "SAVING_GROUP_NOTICE_PIN_FAILED");
    }

    @Test void returnsPinnedNoticesEvenWhenNormalPageIsEmpty() {
        var row = new SavingGroupRow(); row.setGroupId(20L); row.setStatus("01");
        when(mapper.selectSavingGroupDetail(20L)).thenReturn(row);
        var pinned = new SavingGroupNoticeSummaryResponse(40L, "고정 공지", "작성자", null);
        when(mapper.selectSavingGroupPinnedNotices(20L)).thenReturn(List.of(pinned));
        var result = service.getSavingGroupDetail(20L, null, new SavingGroupNoticeSearchCondition());
        assertThat(result.getDetail().getPinnedNotices()).containsExactly(pinned);
        assertThat(result.getDetail().getNotices()).isEmpty();
        assertThat(result.getPageMeta().getTotal()).isZero();
    }

    private void target(String status) {
        var row = new SavingGroupJoinTargetRow(); row.setGroupId(20L); row.setStatus(status);
        when(mapper.selectSavingGroupNoticeTargetForUpdate(20L)).thenReturn(row);
    }
    private void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(row);
    }
    private void notice(boolean pinned) {
        when(mapper.selectSavingGroupNoticePinForUpdate(20L, 40L)).thenReturn(new SavingGroupNoticePinRow(40L, pinned));
    }
    private void change(boolean pinned) { service.updateSavingGroupNoticePinned(20L, 40L, 30L, pinned); }
    private void failure(Runnable action, int status, String code) {
        var error = catchThrowableOfType(action::run, ApiException.class);
        assertThat(error.getStatus().value()).isEqualTo(status); assertThat(error.getCode()).isEqualTo(code);
    }
}
