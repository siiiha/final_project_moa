package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinMemberRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsBasicUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsDetailUpdate;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 공개 상세 조회의 역할·관계 오류 정책을 DB 없이 검증한다.
class SmallGroupServiceImplTest {

    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long MEMBER_ID = 30L;
    private static final long CREATOR_MEMBER_ID = 40L;

    @Mock
    private SmallGroupMapper smallGroupMapper;

    @InjectMocks
    private SmallGroupServiceImpl smallGroupService;

    @Test
    void getSmallGroupDetailReturnsPublicDetailForAnonymousUser() {
        mockExistingDetail();

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, null);

        assertThat(response.getMyRole()).isNull();
        assertThat(response.getMemberCount()).isEqualTo(2L);
        assertThat(response.getNotices()).isEmpty();
        verify(smallGroupMapper, never()).selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID);
    }

    @Test
    void getSmallGroupDetailReturnsRecentNoticeSummariesForAnonymousUser() {
        mockExistingDetail();
        List<SmallGroupNoticeSummaryResponse> notices = List.of(
                notice(4L, "네 번째 공지", "운영진", LocalDateTime.of(2026, 10, 4, 10, 0)),
                notice(3L, "세 번째 공지", "모아회원", LocalDateTime.of(2026, 10, 3, 10, 0)),
                notice(2L, "두 번째 공지", "운영진", LocalDateTime.of(2026, 10, 2, 10, 0)),
                notice(1L, "첫 번째 공지", "모아회원", LocalDateTime.of(2026, 10, 1, 10, 0)));
        when(smallGroupMapper.selectRecentSmallGroupNotices(GROUP_ID)).thenReturn(notices);

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, null);

        assertThat(response.getNotices()).containsExactlyElementsOf(notices);
        verify(smallGroupMapper).selectRecentSmallGroupNotices(GROUP_ID);
    }

    @Test
    void getSmallGroupDetailReturnsNullRoleForLoggedInNonMember() {
        mockExistingDetail();
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID)).thenReturn(List.of());

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID);

        assertThat(response.getMyRole()).isNull();
    }

    @Test
    void getSmallGroupDetailReturnsOwnerRoleForApprovedOwner() {
        mockExistingDetail();
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("OWNER", "APPROVED")));

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID);

        assertThat(response.getMyRole()).isEqualTo("OWNER");
    }

    @Test
    void getSmallGroupDetailReturnsMemberRoleForApprovedMember() {
        mockExistingDetail();
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("MEMBER", "APPROVED")));

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID);

        assertThat(response.getMyRole()).isEqualTo("MEMBER");
    }

    @Test
    void getSmallGroupDetailReturnsNullRoleForUnapprovedOwnerOrMember() {
        mockExistingDetail();
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("MEMBER", "PENDING")));

        SmallGroupDetailResponse response = smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID);

        assertThat(response.getMyRole()).isNull();
    }

    @Test
    void getSmallGroupDetailRejectsManagerRegardlessOfMembershipStatus() {
        mockExistingDetailForRoleResolution();

        for (String status : List.of("PENDING", "APPROVED", "REJECTED")) {
            when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                    .thenReturn(List.of(relation("MANAGER", status)));

            ApiException exception = catchThrowableOfType(
                    () -> smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID),
                    ApiException.class);

            assertThat(exception.getCode()).isEqualTo("ACCESS_DENIED");
        }
        verify(smallGroupMapper, never()).selectApprovedSmallGroupMemberCount(GROUP_ID);
    }

    @Test
    void getSmallGroupDetailRejectsDuplicatedMemberRelations() {
        mockExistingDetailForRoleResolution();
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("OWNER", "APPROVED"), relation("MEMBER", "APPROVED")));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, MEMBER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("INTERNAL_ERROR");
        verify(smallGroupMapper, never()).selectApprovedSmallGroupMemberCount(GROUP_ID);
    }

    @Test
    void getSmallGroupDetailReturnsNotFoundWhenNoPublicDetailExists() {
        when(smallGroupMapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupDetail(SMALL_GROUP_ID, null),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
        verify(smallGroupMapper, never()).selectApprovedSmallGroupMemberCount(GROUP_ID);
    }

    @Test
    void getSmallGroupSettingsReturnsCurrentValuesForCreatorAndApprovedOwner() {
        SmallGroupSettingsResponse settings = settingsResponse();
        when(smallGroupMapper.selectSmallGroupSettingsAuthority(SMALL_GROUP_ID))
                .thenReturn(settingsAuthority(MEMBER_ID, MEMBER_ID));
        when(smallGroupMapper.selectSmallGroupCurrentSettings(SMALL_GROUP_ID)).thenReturn(settings);

        SmallGroupSettingsResponse response =
                smallGroupService.getSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID);

        assertThat(response).isSameAs(settings);
        assertThat(response.getApprovedMemberCount()).isEqualTo(2L);
        verify(smallGroupMapper).selectSmallGroupCurrentSettings(SMALL_GROUP_ID);
    }

    @Test
    void getSmallGroupSettingsRejectsUnauthenticatedUser() {
        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupSettings(SMALL_GROUP_ID, null),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
        verify(smallGroupMapper, never()).selectSmallGroupSettingsAuthority(SMALL_GROUP_ID);
    }

    @Test
    void getSmallGroupSettingsReturnsNotFoundForMissingOrDeletedSmallGroup() {
        when(smallGroupMapper.selectSmallGroupSettingsAuthority(SMALL_GROUP_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
        verify(smallGroupMapper, never()).selectSmallGroupCurrentSettings(SMALL_GROUP_ID);
    }

    @Test
    void getSmallGroupSettingsRejectsUserWhoIsNotCreator() {
        when(smallGroupMapper.selectSmallGroupSettingsAuthority(SMALL_GROUP_ID))
                .thenReturn(settingsAuthority(MEMBER_ID + 1, MEMBER_ID));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_SETTINGS_FORBIDDEN");
        verify(smallGroupMapper, never()).selectSmallGroupCurrentSettings(SMALL_GROUP_ID);
    }

    @Test
    void getSmallGroupSettingsRejectsCreatorWithoutApprovedOwnerRelation() {
        when(smallGroupMapper.selectSmallGroupSettingsAuthority(SMALL_GROUP_ID))
                .thenReturn(settingsAuthority(MEMBER_ID, MEMBER_ID + 1));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_SETTINGS_FORBIDDEN");
        verify(smallGroupMapper, never()).selectSmallGroupCurrentSettings(SMALL_GROUP_ID);
    }

    @Test
    void updateSmallGroupSettingsUpdatesProvidedCommonAndDetailFieldsForOwner() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setGroupName("  주말 걷기  ");
        request.setDescription("  함께 걸어요.  ");
        request.setMaxMembers(8);
        request.setLikeCategory("  운동  ");
        request.setMeetDate(LocalDate.of(2026, 11, 1));
        request.setLocation("  한강 공원  ");
        mockSettingsUpdateOwner();
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(2L);

        smallGroupService.updateSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID, request);

        ArgumentCaptor<SmallGroupSettingsBasicUpdate> basicCaptor =
                ArgumentCaptor.forClass(SmallGroupSettingsBasicUpdate.class);
        ArgumentCaptor<SmallGroupSettingsDetailUpdate> detailCaptor =
                ArgumentCaptor.forClass(SmallGroupSettingsDetailUpdate.class);
        verify(smallGroupMapper).updateSmallGroupBasicSettings(basicCaptor.capture());
        verify(smallGroupMapper).updateSmallGroupDetailSettings(detailCaptor.capture());
        assertThat(basicCaptor.getValue().getGroupId()).isEqualTo(GROUP_ID);
        assertThat(basicCaptor.getValue().getGroupName()).isEqualTo("주말 걷기");
        assertThat(basicCaptor.getValue().getDescription()).isEqualTo("함께 걸어요.");
        assertThat(basicCaptor.getValue().getMaxMembers()).isEqualTo(8);
        assertThat(detailCaptor.getValue().getSmallGroupId()).isEqualTo(SMALL_GROUP_ID);
        assertThat(detailCaptor.getValue().getLikeCategory()).isEqualTo("운동");
        assertThat(detailCaptor.getValue().getMeetDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(detailCaptor.getValue().getLocation()).isEqualTo("한강 공원");
    }

    @Test
    void updateSmallGroupSettingsRejectsUnauthenticatedUser() {
        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.updateSmallGroupSettings(
                        SMALL_GROUP_ID, null, new SmallGroupSettingsPatchRequest()),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("UNAUTHORIZED");
        verify(smallGroupMapper, never()).selectSmallGroupSettingsAuthorityForUpdate(SMALL_GROUP_ID);
    }

    @Test
    void updateSmallGroupSettingsReturnsNotFoundForMissingOrDeletedSmallGroup() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setGroupName("주말 걷기");
        when(smallGroupMapper.selectSmallGroupSettingsAuthorityForUpdate(SMALL_GROUP_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.updateSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID, request),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
        verify(smallGroupMapper, never()).selectSmallGroupOwnerMemberIdForUpdate(GROUP_ID);
        verify(smallGroupMapper, never()).updateSmallGroupBasicSettings(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSmallGroupSettingsRejectsUserWhoIsNotCreatorOrApprovedOwner() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setGroupName("주말 걷기");
        when(smallGroupMapper.selectSmallGroupSettingsAuthorityForUpdate(SMALL_GROUP_ID))
                .thenReturn(settingsAuthority(MEMBER_ID + 1, null));
        when(smallGroupMapper.selectSmallGroupOwnerMemberIdForUpdate(GROUP_ID)).thenReturn(MEMBER_ID);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.updateSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID, request),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_SETTINGS_FORBIDDEN");
        verify(smallGroupMapper, never()).updateSmallGroupBasicSettings(org.mockito.ArgumentMatchers.any());
        verify(smallGroupMapper, never()).updateSmallGroupDetailSettings(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSmallGroupSettingsRejectsCapacityBelowApprovedOwnerAndMemberCount() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setMaxMembers(2);
        mockSettingsUpdateOwner();
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(3L);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.updateSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID, request),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_CAPACITY_BELOW_MEMBERS");
        verify(smallGroupMapper, never()).updateSmallGroupBasicSettings(org.mockito.ArgumentMatchers.any());
        verify(smallGroupMapper, never()).updateSmallGroupDetailSettings(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSmallGroupSettingsUpdatesOnlyDetailFieldsWithoutCapacityCount() {
        SmallGroupSettingsPatchRequest request = new SmallGroupSettingsPatchRequest();
        request.setLocation("시민 공원");
        mockSettingsUpdateOwner();

        smallGroupService.updateSmallGroupSettings(SMALL_GROUP_ID, MEMBER_ID, request);

        verify(smallGroupMapper, never()).selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID);
        verify(smallGroupMapper, never()).updateSmallGroupBasicSettings(org.mockito.ArgumentMatchers.any());
        ArgumentCaptor<SmallGroupSettingsDetailUpdate> detailCaptor =
                ArgumentCaptor.forClass(SmallGroupSettingsDetailUpdate.class);
        verify(smallGroupMapper).updateSmallGroupDetailSettings(detailCaptor.capture());
        assertThat(detailCaptor.getValue().getLocation()).isEqualTo("시민 공원");
    }

    @Test
    void requestSmallGroupJoinCreatesPendingMembershipForEligibleUser() {
        mockJoinApplicant();
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(1L);
        when(smallGroupMapper.insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID)).thenReturn(1);

        smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID);

        verify(smallGroupMapper).insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID);
        verify(smallGroupMapper, never()).reactivateLeftSmallGroupMembership(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinRejectsUnavailableOrUnverifiedMember() {
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("SUSPENDED", true, "USER"));

        ApiException unavailableException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(unavailableException.getCode()).isEqualTo("MEMBER_ACCOUNT_UNAVAILABLE");
        verify(smallGroupMapper, never()).selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID);

        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("ACTIVE", false, "USER"));

        ApiException unverifiedException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(unverifiedException.getCode()).isEqualTo("IDENTITY_VERIFICATION_REQUIRED");
    }

    @Test
    void requestSmallGroupJoinRejectsAdminAccount() {
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("ACTIVE", true, "ADMIN"));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_JOIN_ADMIN_NOT_ALLOWED");
        verify(smallGroupMapper, never()).selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID);
    }

    @Test
    void requestSmallGroupJoinRejectsMissingSmallGroup() {
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("ACTIVE", true, "USER"));
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");
        verify(smallGroupMapper, never()).selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinRejectsCreatorOwnerAndManager() {
        mockJoinApplicant();
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(joinTarget(MEMBER_ID, 10));

        ApiException creatorException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(creatorException.getCode()).isEqualTo("SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED");

        for (String role : List.of("OWNER", "MANAGER")) {
            mockJoinApplicant();
            when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                    .thenReturn(relation(role, "APPROVED"));

            ApiException relationException = catchThrowableOfType(
                    () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

            assertThat(relationException.getCode()).isEqualTo("SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED");
        }
        verify(smallGroupMapper, never()).insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinRejectsApprovedAndPendingMembership() {
        mockJoinApplicant();
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                .thenReturn(relation("MEMBER", "APPROVED"));

        ApiException approvedException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(approvedException.getCode()).isEqualTo("ALREADY_JOINED_SMALL_GROUP");

        mockJoinApplicant();
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                .thenReturn(relation("MEMBER", "PENDING"));

        ApiException pendingException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(pendingException.getCode()).isEqualTo("JOIN_REQUEST_ALREADY_PENDING");
    }

    @Test
    void requestSmallGroupJoinRejectsRejectedAndExpelledMembership() {
        for (String status : List.of("REJECTED", "EXPELLED")) {
            mockJoinApplicant();
            when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                    .thenReturn(relation("MEMBER", status));

            ApiException exception = catchThrowableOfType(
                    () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

            assertThat(exception.getCode()).isEqualTo("JOIN_REAPPLICATION_FORBIDDEN");
        }
        verify(smallGroupMapper, never()).insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinReactivatesLeftMembership() {
        mockJoinApplicant();
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                .thenReturn(relation("MEMBER", "LEFT"));
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(1L);
        when(smallGroupMapper.reactivateLeftSmallGroupMembership(GROUP_ID, MEMBER_ID)).thenReturn(1);

        smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID);

        verify(smallGroupMapper).reactivateLeftSmallGroupMembership(GROUP_ID, MEMBER_ID);
        verify(smallGroupMapper, never()).insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinRejectsMissingOrFullCapacity() {
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("ACTIVE", true, "USER"));
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(joinTarget(CREATOR_MEMBER_ID, null));

        ApiException missingCapacityException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(missingCapacityException.getCode()).isEqualTo("SMALL_GROUP_CAPACITY_NOT_CONFIGURED");

        mockJoinApplicant();
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(10L);

        ApiException fullCapacityException = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(fullCapacityException.getCode()).isEqualTo("SMALL_GROUP_CAPACITY_FULL");
        verify(smallGroupMapper, never()).insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID);
    }

    @Test
    void requestSmallGroupJoinConvertsDuplicateKeyException() {
        mockJoinApplicant();
        when(smallGroupMapper.selectApprovedSmallGroupMemberCountForUpdate(GROUP_ID)).thenReturn(1L);
        when(smallGroupMapper.insertSmallGroupJoinRequest(GROUP_ID, MEMBER_ID))
                .thenThrow(new DuplicateKeyException("duplicate group membership"));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.requestSmallGroupJoin(SMALL_GROUP_ID, MEMBER_ID), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("JOIN_REQUEST_DUPLICATED");
    }

    private void mockExistingDetail() {
        when(smallGroupMapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(detailRow());
        when(smallGroupMapper.selectApprovedSmallGroupMemberCount(GROUP_ID)).thenReturn(2L);
        when(smallGroupMapper.selectRecentSmallGroupNotices(GROUP_ID)).thenReturn(List.of());
    }

    private void mockExistingDetailForRoleResolution() {
        when(smallGroupMapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(detailRow());
    }

    private SmallGroupDetailRow detailRow() {
        SmallGroupDetailRow row = new SmallGroupDetailRow();
        row.setSmallGroupId(SMALL_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setGroupName("동네 걷기");
        row.setDescription("함께 걷는 소모임입니다.");
        row.setRegionName("서울");
        row.setLikeCategory("운동");
        row.setMeetDate(LocalDate.of(2026, 11, 1));
        row.setLocation("한강 공원");
        row.setMaxMembers(10);
        row.setCreatorNickname("모아회원");
        row.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        return row;
    }

    private SmallGroupMemberRelationRow relation(String memberRole, String membershipStatus) {
        SmallGroupMemberRelationRow row = new SmallGroupMemberRelationRow();
        row.setMemberRole(memberRole);
        row.setMembershipStatus(membershipStatus);
        return row;
    }

    private void mockJoinApplicant() {
        when(smallGroupMapper.selectSmallGroupJoinMemberForUpdate(MEMBER_ID))
                .thenReturn(joinMember("ACTIVE", true, "USER"));
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(joinTarget(CREATOR_MEMBER_ID, 10));
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID)).thenReturn(null);
    }

    private SmallGroupJoinMemberRow joinMember(
            String accountStatus, boolean identityVerified, String systemRole) {
        SmallGroupJoinMemberRow row = new SmallGroupJoinMemberRow();
        row.setMemberId(MEMBER_ID);
        row.setAccountStatus(accountStatus);
        row.setIdentityVerifiedAt(identityVerified ? LocalDateTime.of(2026, 10, 1, 10, 0) : null);
        row.setSystemRole(systemRole);
        return row;
    }

    private SmallGroupJoinTargetRow joinTarget(Long creatorMemberId, Integer maxMembers) {
        SmallGroupJoinTargetRow row = new SmallGroupJoinTargetRow();
        row.setSmallGroupId(SMALL_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setCreatorMemberId(creatorMemberId);
        row.setMaxMembers(maxMembers);
        return row;
    }

    private SmallGroupSettingsAuthorityRow settingsAuthority(Long creatorMemberId, Long ownerMemberId) {
        SmallGroupSettingsAuthorityRow row = new SmallGroupSettingsAuthorityRow();
        row.setSmallGroupId(SMALL_GROUP_ID);
        row.setGroupId(GROUP_ID);
        row.setGroupCreatorMemberId(creatorMemberId);
        row.setOwnerMemberId(ownerMemberId);
        return row;
    }

    private void mockSettingsUpdateOwner() {
        when(smallGroupMapper.selectSmallGroupSettingsAuthorityForUpdate(SMALL_GROUP_ID))
                .thenReturn(settingsAuthority(MEMBER_ID, null));
        when(smallGroupMapper.selectSmallGroupOwnerMemberIdForUpdate(GROUP_ID)).thenReturn(MEMBER_ID);
    }

    private SmallGroupSettingsResponse settingsResponse() {
        return new SmallGroupSettingsResponse(
                "동네 걷기", "운동", "함께 걷는 소모임입니다.", 10,
                LocalDate.of(2026, 11, 1), "한강 공원", 2L);
    }

    private SmallGroupNoticeSummaryResponse notice(
            Long noticeId, String title, String writerNickname, LocalDateTime createdAt) {
        return new SmallGroupNoticeSummaryResponse(noticeId, title, writerNickname, createdAt);
    }
}
