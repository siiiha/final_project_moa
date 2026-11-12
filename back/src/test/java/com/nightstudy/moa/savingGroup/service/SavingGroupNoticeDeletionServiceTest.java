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

// AI작성: 삭제 실패 시 저장이 호출되지 않는지와 작성자·회원·상태·잠금 순서를 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeDeletionServiceTest {
    @Mock SavingGroupMapper mapper;
    @InjectMocks SavingGroupServiceImpl service;
    void target(String status) {
        var row = new SavingGroupJoinTargetRow(); row.setGroupId(20L); row.setStatus(status);
        when(mapper.selectSavingGroupNoticeTargetForUpdate(10L)).thenReturn(row);
    }
    void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(row);
    }
    void notice(Long writer) {
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L))
                .thenReturn(new SavingGroupNoticeDetailResponse(40L, "제목", "내용", writer, null, null));
    }
    void delete() { service.deleteSavingGroupNotice(10L, 40L, 30L); }
    void failure(String code) {
        assertThat(catchThrowableOfType(this::delete, ApiException.class).getCode()).isEqualTo(code);
        verify(mapper, never()).deleteSavingGroupNotice(any(), any(), any());
    }
    @ParameterizedTest @CsvSource({"OWNER,01", "OWNER,02", "MANAGER,01", "MANAGER,02"})
    void approvedWriterDeletes(String role, String status) {
        target(status); relation(role, "APPROVED"); notice(30L);
        when(mapper.deleteSavingGroupNotice(20L, 40L, 30L)).thenReturn(1);
        delete();
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupNoticeTargetForUpdate(10L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
        order.verify(mapper).selectSavingGroupNoticeForUpdate(20L, 40L);
        order.verify(mapper).deleteSavingGroupNotice(20L, 40L, 30L);
    }
    @ParameterizedTest @ValueSource(strings = {"OWNER", "MANAGER"})
    void otherOperatorCannotDelete(String role) {
        target("01"); relation(role, "APPROVED"); notice(99L); failure("SAVING_GROUP_NOTICE_DELETE_FORBIDDEN");
    }
    @ParameterizedTest @CsvSource({"MEMBER,APPROVED", "OWNER,PENDING", "MANAGER,PENDING", "OWNER,LEFT", "MANAGER,LEFT", "OWNER,EXPELLED", "MANAGER,EXPELLED", "MANAGER,REJECTED"})
    void rejectsInvalidMembership(String role, String status) {
        target("01"); relation(role, status); failure("SAVING_GROUP_NOTICE_DELETE_FORBIDDEN");
    }
    @Test void rejectsNonmemberOrOtherGroupOperator() {
        target("01"); failure("SAVING_GROUP_NOTICE_DELETE_FORBIDDEN");
        verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
    }
    @Test void rejectsMissingOrDeletedGroup() { failure("NOT_FOUND"); }
    @Test void rejectsMissingDeletedOrOtherGroupNotice() {
        target("01"); relation("OWNER", "APPROVED"); failure("SAVING_GROUP_NOTICE_NOT_FOUND");
    }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04", "99"})
    void rejectsInactiveGroup(String status) { target(status); failure("SAVING_GROUP_ACTIVITY_RESTRICTED"); }
    @ParameterizedTest @ValueSource(ints = {0, 2})
    void rejectsUnexpectedWriteCount(int count) {
        target("01"); relation("OWNER", "APPROVED"); notice(30L);
        when(mapper.deleteSavingGroupNotice(20L, 40L, 30L)).thenReturn(count);
        assertThat(catchThrowableOfType(this::delete, ApiException.class).getCode()).isEqualTo("SAVING_GROUP_NOTICE_DELETE_FAILED");
    }
    @Test void rejectsUnauthenticatedMember() {
        assertThat(catchThrowableOfType(() -> service.deleteSavingGroupNotice(10L, 40L, null), ApiException.class).getCode()).isEqualTo("UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }
    // AI작성: 같은 Service에서 수정→삭제 이후 미삭제 조회가 없으면 재수정·재삭제 모두 거절하는 연계 동작을 검증한다.
    @Test void editThenDeleteRejectsFurtherChanges() {
        target("01"); relation("OWNER", "APPROVED");
        var original = new SavingGroupNoticeDetailResponse(40L, "원본", "내용", 30L, null, null);
        when(mapper.selectSavingGroupNoticeForUpdate(20L, 40L)).thenReturn(original, original, null, null);
        when(mapper.updateSavingGroupNotice(20L, 40L, "수정", "내용")).thenReturn(1);
        when(mapper.deleteSavingGroupNotice(20L, 40L, 30L)).thenReturn(1);
        assertThat(service.updateSavingGroupNotice(10L, 40L, 30L, "수정", "내용")).isEqualTo(40L);
        delete();
        assertThat(catchThrowableOfType(() -> service.updateSavingGroupNotice(10L, 40L, 30L, "재수정", "내용"), ApiException.class).getCode())
                .isEqualTo("SAVING_GROUP_NOTICE_NOT_FOUND");
        assertThat(catchThrowableOfType(this::delete, ApiException.class).getCode()).isEqualTo("SAVING_GROUP_NOTICE_NOT_FOUND");
        verify(mapper, times(1)).deleteSavingGroupNotice(20L, 40L, 30L);
        verify(mapper, times(1)).updateSavingGroupNotice(any(), any(), any(), any());
    }
    @ParameterizedTest @CsvSource({"0,40", "-1,40", "20,0", "20,-1"})
    void rejectsInvalidPath(long groupId, long noticeId) {
        assertThat(catchThrowableOfType(() -> service.deleteSavingGroupNotice(groupId, noticeId, 30L), ApiException.class).getCode()).isEqualTo("BAD_REQUEST");
        verifyNoInteractions(mapper);
    }
}
