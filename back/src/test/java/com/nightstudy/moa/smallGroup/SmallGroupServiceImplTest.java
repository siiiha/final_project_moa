package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
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
        mockExistingDetail();

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
        mockExistingDetail();
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

    private void mockExistingDetail() {
        when(smallGroupMapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(detailRow());
        when(smallGroupMapper.selectApprovedSmallGroupMemberCount(GROUP_ID)).thenReturn(2L);
        when(smallGroupMapper.selectRecentSmallGroupNotices(GROUP_ID)).thenReturn(List.of());
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
