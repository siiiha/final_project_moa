package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.lenient;
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
// AI작성: 소모임 가입 신청 거절의 개설자 권한·소속·상태·조건부 갱신을 DB 없이 검증한다.
class SmallGroupJoinRejectionServiceTest {

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
    void rejectSmallGroupJoinChangesOnlyPendingRequestToRejected() {
        mockJoinRequest(OWNER_ID, GROUP_ID, "PENDING");
        when(smallGroupMapper.rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID)).thenReturn(1);

        smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID);

        verify(smallGroupMapper).rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID);
        verify(smallGroupMapper, never()).insertSmallGroupJoinRequest(GROUP_ID, APPLICANT_ID);
        verify(smallGroupMapper, never()).approveSmallGroupJoinRequest(REQUEST_ID, GROUP_ID, OWNER_ID);
    }

    @Test
    void rejectSmallGroupJoinRejectsNonCreator() {
        mockJoinRequest(OWNER_ID, GROUP_ID, "PENDING");

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID + 1),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_OWNER_REQUIRED");
        verify(smallGroupMapper, never()).rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSmallGroupJoinRejectsCreatorWithoutApprovedOwnerRelation() {
        mockJoinRequest(OWNER_ID, GROUP_ID, "PENDING");
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, OWNER_ID))
                .thenReturn(member("MEMBER", "APPROVED"));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_OWNER_REQUIRED");
        verify(smallGroupMapper, never()).rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSmallGroupJoinRejectsMissingSmallGroupAndJoinRequest() {
        ApiException requestException = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);
        assertThat(requestException.getCode()).isEqualTo("JOIN_REQUEST_NOT_FOUND");

        when(smallGroupMapper.selectSmallGroupJoinApprovalTarget(REQUEST_ID))
                .thenReturn(request(GROUP_ID, "PENDING"));
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(APPLICANT_ID)).thenReturn(applicant());
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(null);

        ApiException groupException = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);
        assertThat(groupException.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
    }

    @Test
    void rejectSmallGroupJoinRejectsRequestForAnotherSmallGroup() {
        mockJoinRequest(OWNER_ID, GROUP_ID + 1, "PENDING");

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_FOUND");
        verify(smallGroupMapper, never()).rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSmallGroupJoinRejectsAlreadyProcessedRequest() {
        mockJoinRequest(OWNER_ID, GROUP_ID, "APPROVED");

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_NOT_PENDING");
        verify(smallGroupMapper, never()).rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID);
    }

    @Test
    void rejectSmallGroupJoinFailsWhenPendingRequestChangesBeforeUpdate() {
        mockJoinRequest(OWNER_ID, GROUP_ID, "PENDING");
        when(smallGroupMapper.rejectSmallGroupJoinRequest(REQUEST_ID, GROUP_ID)).thenReturn(0);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.rejectSmallGroupJoin(SMALL_GROUP_ID, REQUEST_ID, OWNER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_REJECTION_FAILED");
    }

    private void mockJoinRequest(long ownerId, long requestGroupId, String requestStatus) {
        when(smallGroupMapper.selectSmallGroupJoinApprovalTarget(REQUEST_ID))
                .thenReturn(request(requestGroupId, requestStatus));
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(APPLICANT_ID)).thenReturn(applicant());
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(target(ownerId));
        when(smallGroupMapper.selectSmallGroupJoinApprovalTargetForUpdate(REQUEST_ID))
                .thenReturn(request(requestGroupId, requestStatus));
        // AI작성: 권한 검증 전에 종료되는 소속·비개설자 시나리오에서도 같은 공통 Mock을 재사용한다.
        lenient().when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, ownerId))
                .thenReturn(member("OWNER", "APPROVED"));
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

    private SmallGroupJoinTargetRow target(long creatorMemberId) {
        SmallGroupJoinTargetRow row = new SmallGroupJoinTargetRow();
        row.setSmallGroupId(SMALL_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setCreatorMemberId(creatorMemberId);
        return row;
    }

    private SmallGroupMemberRelationRow member(String memberRole, String membershipStatus) {
        SmallGroupMemberRelationRow row = new SmallGroupMemberRelationRow();
        row.setMemberRole(memberRole);
        row.setMembershipStatus(membershipStatus);
        return row;
    }
}
