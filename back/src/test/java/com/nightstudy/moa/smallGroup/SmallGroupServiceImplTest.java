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
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
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
        verify(smallGroupMapper, never()).selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID);
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

    private void mockExistingDetail() {
        when(smallGroupMapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(detailRow());
        when(smallGroupMapper.selectApprovedSmallGroupMemberCount(GROUP_ID)).thenReturn(2L);
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
}
