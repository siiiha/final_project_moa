package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 공지 작성의 모임·역할·회원 상태 검증과 저장 데이터를 DB 없이 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeCreationServiceTest {
    private static final long SAVING_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long MEMBER_ID = 30L;
    @Mock private SavingGroupMapper mapper;
    @InjectMocks private SavingGroupServiceImpl service;

    @ParameterizedTest
    @CsvSource({"OWNER,01", "OWNER,02", "MANAGER,01", "MANAGER,02"})
    void approvedOperatorsCanWrite(String role, String status) {
        target(status);
        relation(role, "APPROVED");
        when(mapper.insertSavingGroupNotice(any())).thenReturn(1);
        write();
        ArgumentCaptor<SavingGroupNoticeCreateRow> saved = ArgumentCaptor.forClass(SavingGroupNoticeCreateRow.class);
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupJoinTargetForUpdate(SAVING_GROUP_ID);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(GROUP_ID, MEMBER_ID);
        order.verify(mapper).insertSavingGroupNotice(saved.capture());
        assertThat(saved.getValue().getGroupId()).isEqualTo(GROUP_ID);
        assertThat(saved.getValue().getWriterId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getValue().getTitle()).isEqualTo("공지 제목");
        assertThat(saved.getValue().getContent()).isEqualTo("공지 내용");
    }

    @ParameterizedTest
    @CsvSource({"MEMBER,APPROVED", "OWNER,PENDING", "MANAGER,PENDING", "OWNER,LEFT",
            "MANAGER,LEFT", "OWNER,REJECTED", "MANAGER,EXPELLED", "UNKNOWN,APPROVED"})
    void rejectsInvalidMembership(String role, String status) {
        target("01");
        relation(role, status);
        failure("SAVING_GROUP_NOTICE_WRITE_FORBIDDEN", 403);
        verify(mapper, never()).insertSavingGroupNotice(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"비회원", "다른 모임 OWNER", "다른 모임 MANAGER"})
    void rejectsUsersWithoutMembershipInTargetGroup(String scenario) {
        target("01");
        // AI작성: 타 모임의 역할과 무관하게 대상 groupId의 관계가 없으면 권한을 부여하지 않는다.
        failure("SAVING_GROUP_NOTICE_WRITE_FORBIDDEN", 403);
        verify(mapper).selectSavingGroupMembershipForUpdate(GROUP_ID, MEMBER_ID);
        verify(mapper, never()).insertSavingGroupNotice(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"00", "03", "04", "UNKNOWN"})
    void rejectsUnavailableGroupStatus(String status) {
        target(status);
        failure("SAVING_GROUP_ACTIVITY_RESTRICTED", 409);
        verify(mapper, never()).selectSavingGroupMembershipForUpdate(any(), any());
        verify(mapper, never()).insertSavingGroupNotice(any());
    }

    @Test
    void rejectsMissingOrDeletedGroup() {
        failure("SAVING_GROUP_NOT_FOUND", 404);
        verify(mapper, never()).insertSavingGroupNotice(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsUnauthenticatedMember(Long memberId) {
        ApiException error = catchThrowableOfType(
                () -> service.createSavingGroupNotice(SAVING_GROUP_ID, memberId, "제목", "내용"), ApiException.class);
        assertThat(error.getCode()).isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidSavingGroupId(Long savingGroupId) {
        ApiException error = catchThrowableOfType(
                () -> service.createSavingGroupNotice(savingGroupId, MEMBER_ID, "제목", "내용"), ApiException.class);
        assertThat(error.getCode()).isEqualTo("BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void rejectsUnexpectedInsertCount(int count) {
        target("01");
        relation("OWNER", "APPROVED");
        when(mapper.insertSavingGroupNotice(any())).thenReturn(count);
        failure("SAVING_GROUP_NOTICE_WRITE_FAILED", 500);
    }

    private void write() {
        service.createSavingGroupNotice(SAVING_GROUP_ID, MEMBER_ID, " 공지 제목 ", " 공지 내용 ");
    }

    private void failure(String code, int status) {
        ApiException error = catchThrowableOfType(this::write, ApiException.class);
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getStatus().value()).isEqualTo(status);
    }

    private void target(String status) {
        SavingGroupJoinTargetRow row = new SavingGroupJoinTargetRow();
        row.setSavingGroupId(SAVING_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setStatus(status);
        when(mapper.selectSavingGroupJoinTargetForUpdate(SAVING_GROUP_ID)).thenReturn(row);
    }

    private void relation(String role, String status) {
        SavingGroupMembershipRelationRow row = new SavingGroupMembershipRelationRow();
        row.setMemberRole(role);
        row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(GROUP_ID, MEMBER_ID)).thenReturn(row);
    }
}
