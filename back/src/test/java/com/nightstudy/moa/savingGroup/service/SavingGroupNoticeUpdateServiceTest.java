package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: DB 없이 승인 운영진 권한·상태·소속·저장 결과를 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeUpdateServiceTest {
    @Mock SavingGroupMapper mapper;
    @InjectMocks SavingGroupServiceImpl service;
    void target(String status) {
        var row = new SavingGroupJoinTargetRow(); row.setGroupId(20L); row.setSavingGroupId(10L); row.setStatus(status);
        when(mapper.selectSavingGroupNoticeTargetForUpdate(20L)).thenReturn(row);
    }
    void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(row);
    }
    Long update() { return service.updateSavingGroupNotice(20L, 40L, 30L, " 새 제목 ", " 새 내용 "); }
    void failure(String code) {
        var error = catchThrowableOfType(this::update, ApiException.class);
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(code);
        verify(mapper, never()).updateSavingGroupNotice(any(), any(), any(), any());
    }
    @ParameterizedTest @CsvSource({"OWNER,01", "OWNER,02", "MANAGER,01", "MANAGER,02"})
    void approvedWriterCanEdit(String role, String status) {
        target(status); relation(role, "APPROVED");
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L))
                .thenReturn(new SavingGroupNoticeDetailResponse(40L, "이전 제목", "이전 내용", 30L, null, null));
        when(mapper.updateSavingGroupNotice(20L, 40L, "새 제목", "새 내용")).thenReturn(1);
        assertThat(update()).isEqualTo(40L);
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupNoticeTargetForUpdate(20L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
        order.verify(mapper).selectSavingGroupNoticeForUpdate(20L, 40L);
        order.verify(mapper).updateSavingGroupNotice(20L, 40L, "새 제목", "새 내용");
    }
    @ParameterizedTest @CsvSource({"MEMBER,APPROVED", "OWNER,PENDING", "MANAGER,PENDING", "OWNER,LEFT", "MANAGER,LEFT", "OWNER,EXPELLED", "MANAGER,EXPELLED", "MANAGER,REJECTED"})
    void rejectsMembership(String role, String status) {
        target("01"); relation(role, status); failure("SAVING_GROUP_NOTICE_EDIT_FORBIDDEN");
    }
    @Test void rejectsNonmemberAndOtherGroupOperator() {
        target("01"); failure("SAVING_GROUP_NOTICE_EDIT_FORBIDDEN");
        verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
    }
    // AI작성: 운영진 권한만으로 타인의 공지를 변경하는 요청을 차단한다.
    @ParameterizedTest @ValueSource(strings = {"OWNER", "MANAGER"})
    void rejectsOtherWriterEvenForOperator(String role) {
        target("01"); relation(role, "APPROVED");
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L))
                .thenReturn(new SavingGroupNoticeDetailResponse(40L, "제목", "내용", 99L, null, null));
        failure("SAVING_GROUP_NOTICE_EDIT_FORBIDDEN");
    }
    @Test void rejectsMissingOrDeletedGroup() { failure("NOT_FOUND"); }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04", "99"})
    void rejectsInactiveGroup(String status) { target(status); failure("SAVING_GROUP_ACTIVITY_RESTRICTED"); }
    @Test void rejectsMissingDeletedOrOtherGroupNotice() {
        target("01"); relation("OWNER", "APPROVED"); failure("SAVING_GROUP_NOTICE_NOT_FOUND");
        verify(mapper).selectSavingGroupNoticeForUpdate(20L, 40L);
    }
    @Test void unchangedRequestSucceedsWithoutWrite() {
        target("01"); relation("OWNER", "APPROVED");
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L))
                .thenReturn(new SavingGroupNoticeDetailResponse(40L, "새 제목", "새 내용", 30L, null, null));
        assertThat(update()).isEqualTo(40L);
        verify(mapper, never()).updateSavingGroupNotice(any(), any(), any(), any());
    }
    @Test void rejectsSaveFailure() {
        target("01"); relation("OWNER", "APPROVED");
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L))
                .thenReturn(new SavingGroupNoticeDetailResponse(40L, "이전", "내용", 30L, null, null));
        assertThat(catchThrowableOfType(this::update, ApiException.class).getCode()).isEqualTo("SAVING_GROUP_NOTICE_EDIT_FAILED");
    }
    @Test void rejectsMissingAuthenticationBeforeQuery() {
        assertThat(catchThrowableOfType(() -> service.updateSavingGroupNotice(20L, 40L, null, "제목", "내용"), ApiException.class).getCode())
                .isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }
    // AI작성: 인증 및 경로 오류는 DB 접근 전에 거절한다.
    @ParameterizedTest @ValueSource(longs = {0, -1})
    void rejectsInvalidMemberId(long memberId) {
        assertThat(catchThrowableOfType(() -> service.updateSavingGroupNotice(20L, 40L, memberId, "제목", "내용"), ApiException.class).getCode())
                .isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }
    @ParameterizedTest @CsvSource({"0,40", "-1,40", "20,0", "20,-1"})
    void rejectsInvalidPath(long groupId, long noticeId) {
        assertThat(catchThrowableOfType(() -> service.updateSavingGroupNotice(groupId, noticeId, 30L, "제목", "내용"), ApiException.class).getCode())
                .isEqualTo("BAD_REQUEST");
        verifyNoInteractions(mapper);
    }
}
