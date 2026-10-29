package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 개설 신청의 계주 대기 관계 생성과 관리자 승인·반려 상태 전환을 DB 없이 검증한다.
class SavingGroupCreationServiceImplTest {

    @Mock
    private SavingGroupMapper savingGroupMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private SavingGroupCreationServiceImpl savingGroupCreationService;

    @Test
    void applyForCreationAddsPendingOwnerMembership() {
        SavingGroupCreationRequest request = creationRequest();
        when(savingGroupMapper.selectRegionIdByName("서울")).thenReturn(1L);
        when(savingGroupMapper.existsMember(10L)).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<SavingGroupCreationRequest>getArgument(0).setGroupId(100L);
            return 1;
        }).when(savingGroupMapper).insertGroups(any(SavingGroupCreationRequest.class));
        when(savingGroupMapper.insertSavingGroup(request)).thenReturn(1);
        when(savingGroupMapper.insertSavingGroupCreationOwnerMembership(100L, 10L)).thenReturn(1);
        when(savingGroupMapper.insertSavingGroupCreationApproval(100L)).thenReturn(1);

        assertThat(savingGroupCreationService.applyForCreationSavingGroup(request)).isTrue();

        verify(savingGroupMapper).insertSavingGroupCreationOwnerMembership(100L, 10L);
    }

    @Test
    void approveCreationChangesOwnerMembershipToApproved() {
        SavingGroupCreationApplicationDetailResponse application = pendingApplication(100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.approveSavingGroupCreationApplication(1L)).thenReturn(1);
        when(savingGroupMapper.publishApprovedSavingGroup(100L)).thenReturn(1);
        when(savingGroupMapper.approveSavingGroupCreationOwnerMembership(100L, 20L)).thenReturn(1);

        savingGroupCreationService.approveCreationApplication(1L, 20L);

        verify(savingGroupMapper).approveSavingGroupCreationOwnerMembership(100L, 20L);
    }

    @Test
    void rejectCreationChangesOwnerMembershipToRejected() {
        SavingGroupCreationApplicationDetailResponse application = pendingApplication(100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.rejectSavingGroupCreationApplication(1L, "사유")).thenReturn(1);
        when(savingGroupMapper.rejectSavingGroupCreationOwnerMembership(100L)).thenReturn(1);

        savingGroupCreationService.rejectCreationApplication(1L, "사유");

        verify(savingGroupMapper).rejectSavingGroupCreationOwnerMembership(100L);
    }

    @Test
    // AI작성: 삭제 승인은 심사 이력을 먼저 처리하고 같은 트랜잭션에서 계모임을 소프트 삭제한다.
    void approveDeletionSoftDeletesSavingGroup() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.selectSavingGroupBalance(100L)).thenReturn(BigDecimal.ZERO);
        when(savingGroupMapper.approveSavingGroupDeletionApplication(1L)).thenReturn(1);
        when(savingGroupMapper.softDeleteSavingGroup(100L)).thenReturn(1);

        savingGroupCreationService.approveCreationApplication(1L, 20L);

        verify(savingGroupMapper).approveSavingGroupDeletionApplication(1L);
        verify(savingGroupMapper).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 진행 중인 회차가 있으면 삭제 이력과 원본 계모임을 모두 변경하지 않는다.
    void approveDeletionRejectsActiveSavingGroup() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.existsActiveSavingGroup(100L)).thenReturn(true);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_DELETE_ACTIVE_ROUND");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 공동자금이 남아 있으면 삭제 승인을 차단한다.
    void approveDeletionRejectsRemainingBalance() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.selectSavingGroupBalance(100L)).thenReturn(BigDecimal.ONE);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_DELETE_BALANCE_REMAINS");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 승인 또는 대기 중인 출금 요청은 삭제 전에 먼저 처리해야 한다.
    void approveDeletionRejectsPendingPayout() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.selectSavingGroupBalance(100L)).thenReturn(BigDecimal.ZERO);
        when(savingGroupMapper.existsPendingPayout(100L)).thenReturn(true);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_DELETE_PAYOUT_PENDING");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 삭제 심사 반려는 반려 사유만 저장하고 원본 계모임을 삭제하지 않는지 검증한다.
    void rejectDeletionKeepsSavingGroup() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.rejectSavingGroupDeletionApplication(1L, "공동자금 정리 필요")).thenReturn(1);

        savingGroupCreationService.rejectCreationApplication(1L, " 공동자금 정리 필요 ");

        verify(savingGroupMapper).rejectSavingGroupDeletionApplication(1L, "공동자금 정리 필요");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 이미 승인된 삭제 신청은 심사 상태와 계모임을 다시 변경하지 않는지 검증한다.
    void approveDeletionRejectsAlreadyApprovedApplication() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        application.setStatus("APPROVED");
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_APPLICATION_ALREADY_PROCESSED");
        verify(savingGroupMapper, never()).approveSavingGroupDeletionApplication(1L);
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 이미 반려된 삭제 신청은 반려 사유와 계모임을 다시 변경하지 않는지 검증한다.
    void rejectDeletionRejectsAlreadyRejectedApplication() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        application.setStatus("REJECTED");
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.rejectCreationApplication(1L, "재반려 사유"), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_APPLICATION_ALREADY_PROCESSED");
        verify(savingGroupMapper, never()).rejectSavingGroupDeletionApplication(1L, "재반려 사유");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 심사 승인 상태 변경이 실패하면 계모임 소프트 삭제를 이어서 실행하지 않는지 검증한다.
    void approveDeletionDoesNotDeleteWhenApprovalUpdateFails() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.selectSavingGroupBalance(100L)).thenReturn(BigDecimal.ZERO);
        when(savingGroupMapper.approveSavingGroupDeletionApplication(1L)).thenReturn(0);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_APPLICATION_PROCESS_FAILED");
        verify(savingGroupMapper, never()).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 소프트 삭제가 실패하면 트랜잭션 롤백을 유발할 처리 실패 예외가 발생하는지 검증한다.
    void approveDeletionFailsWhenSoftDeleteFails() {
        SavingGroupCreationApplicationDetailResponse application = pendingDeletionApplication(1L, 100L);
        when(savingGroupMapper.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(savingGroupMapper.selectSavingGroupBalance(100L)).thenReturn(BigDecimal.ZERO);
        when(savingGroupMapper.approveSavingGroupDeletionApplication(1L)).thenReturn(1);
        when(savingGroupMapper.softDeleteSavingGroup(100L)).thenReturn(0);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupCreationService.approveCreationApplication(1L, 20L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_APPLICATION_PROCESS_FAILED");
        verify(savingGroupMapper).approveSavingGroupDeletionApplication(1L);
        verify(savingGroupMapper).softDeleteSavingGroup(100L);
    }

    @Test
    // AI작성: 관리자 목록의 삭제 유형 필터가 MyBatis 조회 조건까지 전달되는지 검증한다.
    void getApplicationsPassesDeleteTypeFilter() {
        SavingGroupCreationApplicationSearchCondition condition = new SavingGroupCreationApplicationSearchCondition();
        condition.setType("DELETE");
        when(savingGroupMapper.selectSavingGroupCreationApplicationCount(any())).thenReturn(0);

        savingGroupCreationService.getCreationApplicationPageMeta(condition);

        ArgumentCaptor<SavingGroupCreationApplicationListCondition> captor =
                ArgumentCaptor.forClass(SavingGroupCreationApplicationListCondition.class);
        verify(savingGroupMapper).selectSavingGroupCreationApplicationCount(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("DELETE");
    }

    private SavingGroupCreationRequest creationRequest() {
        SavingGroupCreationRequest request = new SavingGroupCreationRequest();
        request.setMemberId(10L);
        request.setGroupName("테스트 계모임");
        request.setRegionName("서울");
        request.setMaxMembers(10);
        request.setDescription("계모임 소개");
        request.setGroupType("친목계");
        request.setDuesAmount(new BigDecimal("10000"));
        request.setDuesDay(10);
        return request;
    }

    private SavingGroupCreationApplicationDetailResponse pendingApplication(Long groupId) {
        SavingGroupCreationApplicationDetailResponse application = new SavingGroupCreationApplicationDetailResponse();
        application.setGroupId(groupId);
        application.setStatus("PENDING");
        return application;
    }

    private SavingGroupCreationApplicationDetailResponse pendingDeletionApplication(Long approvalId, Long groupId) {
        SavingGroupCreationApplicationDetailResponse application = pendingApplication(groupId);
        application.setApprovalId(approvalId);
        application.setType("DELETE");
        return application;
    }
}
