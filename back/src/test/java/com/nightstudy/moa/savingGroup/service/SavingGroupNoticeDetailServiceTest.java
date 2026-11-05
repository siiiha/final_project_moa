package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: DB 없이 승인 역할·회원 상태와 공지 조회 전 검증 순서 및 응답을 확인한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeDetailServiceTest {
    private static final long GROUP_ID = 20L;
    private static final long NOTICE_ID = 40L;
    private static final long MEMBER_ID = 30L;
    @Mock private SavingGroupMapper mapper;
    @InjectMocks private SavingGroupServiceImpl service;

    @ParameterizedTest
    @CsvSource({"OWNER,01", "OWNER,02", "MANAGER,01", "MANAGER,02", "MEMBER,01", "MEMBER,02"})
    void approvedMembersCanReadAllResponseFields(String role, String status) {
        target(status);
        relation(role, "APPROVED");
        LocalDateTime createdAt = LocalDateTime.of(2026, 11, 4, 10, 0);
        var notice = new SavingGroupNoticeDetailResponse(NOTICE_ID, "공지 제목", "공지 내용", 50L, "테스트닉네임", createdAt);
        when(mapper.selectSavingGroupNoticeDetail(GROUP_ID, NOTICE_ID)).thenReturn(notice);

        var result = read();
        assertThat(result.getNoticeId()).isEqualTo(NOTICE_ID);
        assertThat(result.getTitle()).isEqualTo("공지 제목");
        assertThat(result.getContent()).isEqualTo("공지 내용");
        assertThat(result.getWriterId()).isEqualTo(50L);
        assertThat(result.getWriterNickname()).isEqualTo("테스트닉네임");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupDetail(GROUP_ID);
        order.verify(mapper).selectSavingGroupMembership(GROUP_ID, MEMBER_ID);
        order.verify(mapper).selectSavingGroupNoticeDetail(GROUP_ID, NOTICE_ID);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsUnauthenticatedMember(Long memberId) {
        failure(() -> service.getSavingGroupNoticeDetail(GROUP_ID, NOTICE_ID, memberId), "UNAUTHORIZED", 401);
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @CsvSource({",40", "0,40", "-1,40", "20,", "20,0", "20,-1"})
    void rejectsInvalidPathIds(Long groupId, Long noticeId) {
        failure(() -> service.getSavingGroupNoticeDetail(groupId, noticeId, MEMBER_ID), "BAD_REQUEST", 400);
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsGroupExcludedByExistingDetailQuery() {
        // AI작성: 기존 SQL에서 존재하지 않음·삭제·비활동 상태는 모두 조회 결과 null이다. SQL 자체 검증은 별도다.
        failure(this::read, "NOT_FOUND", 404);
        verify(mapper).selectSavingGroupDetail(GROUP_ID);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @CsvSource({"OWNER,PENDING", "MANAGER,REJECTED", "MEMBER,LEFT", "OWNER,EXPELLED"})
    void rejectsUnapprovedMembership(String role, String status) {
        target("01");
        relation(role, status);
        failure(this::read, "SAVING_GROUP_NOTICE_READ_FORBIDDEN", 403);
        verify(mapper, never()).selectSavingGroupNoticeDetail(any(), any());
    }

    @Test
    void rejectsMissingMembershipInTargetGroup() {
        target("01");
        failure(this::read, "SAVING_GROUP_NOTICE_READ_FORBIDDEN", 403);
        verify(mapper).selectSavingGroupMembership(GROUP_ID, MEMBER_ID);
        verify(mapper, never()).selectSavingGroupNoticeDetail(any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"UNKNOWN"})
    void rejectsUndefinedApprovedRole(String role) {
        target("01");
        relation(role, "APPROVED");
        failure(this::read, "INTERNAL_ERROR", 500);
        verify(mapper, never()).selectSavingGroupNoticeDetail(any(), any());
    }

    @ParameterizedTest
    @ValueSource(longs = {NOTICE_ID, 99L})
    void rejectsMissingOrOtherGroupNotice(long noticeId) {
        target("01");
        relation("MEMBER", "APPROVED");
        // AI작성: 소속 불일치와 공지 없음은 두 ID를 조건으로 하는 Mapper의 null 결과로 표현한다.
        failure(() -> service.getSavingGroupNoticeDetail(GROUP_ID, noticeId, MEMBER_ID), "SAVING_GROUP_NOTICE_NOT_FOUND", 404);
        verify(mapper).selectSavingGroupNoticeDetail(GROUP_ID, noticeId);
    }

    private SavingGroupNoticeDetailResponse read() {
        return service.getSavingGroupNoticeDetail(GROUP_ID, NOTICE_ID, MEMBER_ID);
    }

    private void failure(Runnable action, String code, int status) {
        ApiException error = catchThrowableOfType(action::run, ApiException.class);
        assertThat(error.getCode()).isEqualTo(code);
        assertThat(error.getStatus().value()).isEqualTo(status);
    }

    private void target(String status) {
        var row = new SavingGroupRow();
        row.setGroupId(GROUP_ID);
        row.setSavingGroupId(10L);
        row.setStatus(status);
        when(mapper.selectSavingGroupDetail(GROUP_ID)).thenReturn(row);
    }

    private void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow();
        row.setMemberRole(role);
        row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(GROUP_ID, MEMBER_ID)).thenReturn(row);
    }
}
