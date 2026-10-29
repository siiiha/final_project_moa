package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinApprovalTargetRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinMemberRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinTargetRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMembershipRelationRow;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 가입 신청 거절 시 계주 권한·대기 상태·조건부 상태 변경을 DB 없이 검증한다.
class SavingGroupJoinRejectionServiceTest {

    private static final long SAVING_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long OWNER_ID = 30L;
    private static final long REQUEST_ID = 40L;
    private static final long APPLICANT_ID = 50L;

    @Mock
    private SavingGroupMapper savingGroupMapper;

    @InjectMocks
    private SavingGroupServiceImpl savingGroupService;

    @Test
    void rejectSavingGroupJoinChangesOnlyPendingRequestToRejected() {
        mockJoinRequest(OWNER_ID, "PENDING", GROUP_ID);
        when(savingGroupMapper.rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID)).thenReturn(1);

        savingGroupService.rejectSavingGroupJoin(SAVING_GROUP_ID, REQUEST_ID, OWNER_ID);

        verify(savingGroupMapper).rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSavingGroupJoinRejectsNonOwner() {
        mockJoinRequest(OWNER_ID, "PENDING", GROUP_ID);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.rejectSavingGroupJoin(SAVING_GROUP_ID, REQUEST_ID, OWNER_ID + 1),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SAVING_GROUP_OWNER_REQUIRED");
        verify(savingGroupMapper, never()).rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSavingGroupJoinRejectsAlreadyProcessedRequest() {
        mockJoinRequest(OWNER_ID, "APPROVED", GROUP_ID);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.rejectSavingGroupJoin(SAVING_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_PENDING");
        verify(savingGroupMapper, never()).rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSavingGroupJoinRejectsRequestFromAnotherSavingGroup() {
        mockJoinRequest(OWNER_ID, "PENDING", GROUP_ID + 1);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.rejectSavingGroupJoin(SAVING_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_FOUND");
        verify(savingGroupMapper, never()).rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSavingGroupJoinFailsWhenPendingRequestChangedBeforeUpdate() {
        mockJoinRequest(OWNER_ID, "PENDING", GROUP_ID);
        when(savingGroupMapper.rejectSavingGroupJoinRequest(REQUEST_ID, GROUP_ID)).thenReturn(0);

        ApiException exception = catchThrowableOfType(
                () -> savingGroupService.rejectSavingGroupJoin(SAVING_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_REJECTION_FAILED");
    }

    private void mockJoinRequest(long ownerId, String requestStatus, long requestGroupId) {
        SavingGroupJoinApprovalTargetRow identified = request(requestGroupId, requestStatus);
        SavingGroupJoinApprovalTargetRow locked = request(requestGroupId, requestStatus);
        when(savingGroupMapper.selectSavingGroupJoinApprovalTarget(REQUEST_ID)).thenReturn(identified);
        when(savingGroupMapper.selectSavingGroupJoinMemberForUpdate(APPLICANT_ID)).thenReturn(applicant());
        when(savingGroupMapper.selectSavingGroupJoinTargetForUpdate(SAVING_GROUP_ID)).thenReturn(target(ownerId));
        when(savingGroupMapper.selectSavingGroupJoinApprovalTargetForUpdate(REQUEST_ID)).thenReturn(locked);
        when(savingGroupMapper.selectSavingGroupMembershipForUpdate(GROUP_ID, ownerId)).thenReturn(owner());
    }

    private SavingGroupJoinApprovalTargetRow request(long groupId, String status) {
        SavingGroupJoinApprovalTargetRow request = new SavingGroupJoinApprovalTargetRow();
        request.setCrewMemberId(REQUEST_ID);
        request.setMemberId(APPLICANT_ID);
        request.setGroupId(groupId);
        request.setMembershipStatus(status);
        return request;
    }

    private SavingGroupJoinMemberRow applicant() {
        SavingGroupJoinMemberRow applicant = new SavingGroupJoinMemberRow();
        applicant.setMemberId(APPLICANT_ID);
        return applicant;
    }

    private SavingGroupJoinTargetRow target(long ownerId) {
        SavingGroupJoinTargetRow target = new SavingGroupJoinTargetRow();
        target.setSavingGroupId(SAVING_GROUP_ID);
        target.setGroupId(GROUP_ID);
        target.setStatus("01");
        target.setOwnerMemberId(ownerId);
        return target;
    }

    private SavingGroupMembershipRelationRow owner() {
        SavingGroupMembershipRelationRow owner = new SavingGroupMembershipRelationRow();
        owner.setMemberRole("OWNER");
        owner.setMembershipStatus("APPROVED");
        return owner;
    }
}
