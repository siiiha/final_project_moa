package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinApprovalTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinMemberRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 소모임 가입 신청 승인의 개설자 권한·소속·상태·정원·조건부 갱신을 DB 없이 검증한다.
class SmallGroupJoinApprovalServiceTest {

    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long OWNER_ID = 30L;
    private static final long REQUEST_ID = 40L;
    private static final long APPLICANT_ID = 50L;

    @Mock
    private SmallGroupMapper smallGroupMapper;

    @InjectMocks
    private SmallGroupServiceImpl smallGroupService;

    @Test
    void approveSmallGroupJoinApprovesOnlyPendingRequestForCreator() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "PENDING", 10);
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(1L);
        when(smallGroupMapper.approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID)).thenReturn(1);

        smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID);

        verify(smallGroupMapper).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    @Test
    void approveSmallGroupJoinRejectsNonCreator() {
        mockApprovalRequest(OWNER_ID + 1, GROUP_ID, "PENDING", 10);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_OWNER_REQUIRED");
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID + 1);
    }

    @Test
    void approveSmallGroupJoinRejectsMissingSmallGroupAndJoinRequest() {
        ApiException requestException = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);
        assertThat(requestException.getCode()).isEqualTo("JOIN_REQUEST_NOT_FOUND");

        when(smallGroupMapper.selectSmallGroupJoinApprovalTarget(REQUEST_ID))
                .thenReturn(request(GROUP_ID, "PENDING"));
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(APPLICANT_ID)).thenReturn(applicant());
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(null);

        ApiException groupException = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);
        assertThat(groupException.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
    }

    @Test
    void approveSmallGroupJoinRejectsRequestForAnotherSmallGroup() {
        mockApprovalRequest(OWNER_ID, GROUP_ID + 1, "PENDING", 10);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_FOUND");
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    @Test
    void approveSmallGroupJoinRejectsAlreadyProcessedRequest() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "APPROVED", 10);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_PENDING");
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    @Test
    void approveSmallGroupJoinRejectsFullCapacity() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "PENDING", 2);
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(2L);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_CAPACITY_FULL");
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    @Test
    void approveSmallGroupJoinFailsWhenPendingRequestChangesBeforeUpdate() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "PENDING", 10);
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(1L);
        when(smallGroupMapper.approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID)).thenReturn(0);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_APPROVAL_FAILED");
    }

    // AI작성: 신청 후 일정이 지나거나 일정이 제거되어도 승인으로 우회할 수 없다.
    @Test
    void approveSmallGroupJoinRejectsElapsedOrMissingMeeting() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "PENDING", 10);
        for (java.time.LocalDate date : new java.time.LocalDate[] {
                java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1), null }) {
            SmallGroupJoinTargetRow target = target(OWNER_ID, 10);
            target.setMeetDate(date);
            when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target);
            ApiException error = catchThrowableOfType(
                    () -> smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID), ApiException.class);
            assertThat(error.getCode()).isEqualTo(date == null
                    ? "SMALL_GROUP_DATE_NOT_CONFIGURED" : "SMALL_GROUP_MEETING_ENDED");
        }
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    // AI작성: 한국 날짜 기준 일정 당일은 가입 승인을 허용한다.
    @Test
    void approveSmallGroupJoinAllowsMeetingToday() {
        mockApprovalRequest(OWNER_ID, GROUP_ID, "PENDING", 10);
        SmallGroupJoinTargetRow target = target(OWNER_ID, 10);
        target.setMeetDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target);
        when(smallGroupMapper.approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID)).thenReturn(1);
        smallGroupService.approveSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID);
        verify(smallGroupMapper).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    private void mockApprovalRequest(long ownerId, long requestGroupId, String requestStatus, int maxMembers) {
        when(smallGroupMapper.selectSmallGroupJoinApprovalTarget(REQUEST_ID))
                .thenReturn(request(requestGroupId, requestStatus));
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(APPLICANT_ID)).thenReturn(applicant());
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(target(ownerId, maxMembers));
        when(smallGroupMapper.selectSmallGroupJoinApprovalTargetForUpdate(REQUEST_ID))
                .thenReturn(request(requestGroupId, requestStatus));
        // AI작성: 소속·개설자 검사에서 먼저 차단되는 경우에는 실행되지 않는 관계 조회를 준비하지 않는다.
        if (ownerId == OWNER_ID && requestGroupId == GROUP_ID) {
            when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, ownerId))
                    .thenReturn(owner());
        }
    }

    private SmallGroupJoinApprovalTargetRow request(long groupId, String membershipStatus) {
        SmallGroupJoinApprovalTargetRow row = new SmallGroupJoinApprovalTargetRow();
        row.setCrewMemberId(REQUEST_ID);
        row.setMemberId(APPLICANT_ID);
        row.setGroupId(groupId);
        row.setMembershipStatus(membershipStatus);
        return row;
    }

    private SmallGroupJoinMemberRow applicant() {
        SmallGroupJoinMemberRow row = new SmallGroupJoinMemberRow();
        row.setMemberId(APPLICANT_ID);
        return row;
    }

    private SmallGroupJoinTargetRow target(long creatorMemberId, int maxMembers) {
        SmallGroupJoinTargetRow row = new SmallGroupJoinTargetRow();
        row.setSmallGroupId(SMALL_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setCreatorMemberId(creatorMemberId);
        // AI작성: 기본 가입 대상은 한국 날짜 기준 내일로 두어 실행일에 영향받지 않는다.
        row.setMeetDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).plusDays(1));
        row.setMaxMembers(maxMembers);
        return row;
    }

    private SmallGroupMemberRelationRow owner() {
        SmallGroupMemberRelationRow row = new SmallGroupMemberRelationRow();
        row.setMemberRole("OWNER");
        row.setMembershipStatus("APPROVED");
        return row;
    }
}
