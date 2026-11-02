package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionVoterNicknameRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 소모임 투표 상세의 회원 권한·결과 공개·마감 후 숨김 정책을 DB 없이 검증한다.
class SmallGroupVoteDetailServiceTest {

    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long VOTE_ID = 30L;
    private static final long MEMBER_ID = 40L;
    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Mock
    private SmallGroupMapper smallGroupMapper;

    @InjectMocks
    private SmallGroupServiceImpl smallGroupService;

    @Test
    void getSmallGroupVoteDetailReturnsPublicVoteResultsForParticipatedMember() {
        mockApprovedMember("MEMBER");
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote(false,
                LocalDateTime.now(KOREA_ZONE_ID).plusHours(1), null));
        when(smallGroupMapper.selectSmallGroupVoteParticipationId(VOTE_ID, MEMBER_ID)).thenReturn(90L);
        when(smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(90L)).thenReturn(List.of(101L));
        when(smallGroupMapper.selectSmallGroupVoteOptionVoterNicknames(VOTE_ID))
                .thenReturn(List.of(voterNickname(101L, "걷기좋아")));
        when(smallGroupMapper.selectSmallGroupVoteDetailOptions(VOTE_ID))
                .thenReturn(List.of(option(101L, "한강공원", 1, 2L), option(102L, "서울숲", 2, 1L)));
        when(smallGroupMapper.selectSmallGroupVoteParticipantCount(VOTE_ID)).thenReturn(3L);

        SmallGroupVoteDetailResponse response =
                smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID);

        assertThat(response.getStatus()).isEqualTo("OPEN");
        assertThat(response.getParticipantCount()).isEqualTo(3L);
        assertThat(response.getMyParticipation().isParticipated()).isTrue();
        assertThat(response.getMyParticipation().getSelectedOptionIds()).containsExactly(101L);
        assertThat(response.getOptions().get(0).getVoteCount()).isEqualTo(2L);
        assertThat(response.getOptions().get(0).getVoterNicknames()).containsExactly("걷기좋아");
    }

    @Test
    void getSmallGroupVoteDetailHidesNicknamesForAnonymousOrNotParticipatedMember() {
        mockApprovedMember("OWNER");
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote(true,
                LocalDateTime.now(KOREA_ZONE_ID).plusHours(1), null));
        when(smallGroupMapper.selectSmallGroupVoteParticipationId(VOTE_ID, MEMBER_ID)).thenReturn(90L);
        when(smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(90L)).thenReturn(List.of(101L));
        when(smallGroupMapper.selectSmallGroupVoteDetailOptions(VOTE_ID))
                .thenReturn(List.of(option(101L, "한강공원", 1, 2L)));
        when(smallGroupMapper.selectSmallGroupVoteParticipantCount(VOTE_ID)).thenReturn(2L);

        SmallGroupVoteDetailResponse response =
                smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID);

        assertThat(response.getOptions().get(0).getVoterNicknames()).isEmpty();
        verify(smallGroupMapper, never()).selectSmallGroupVoteOptionVoterNicknames(VOTE_ID);
    }

    @Test
    void getSmallGroupVoteDetailHidesPublicVoteNicknamesBeforeParticipation() {
        mockApprovedMember("MEMBER");
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote(false,
                LocalDateTime.now(KOREA_ZONE_ID).plusHours(1), null));
        when(smallGroupMapper.selectSmallGroupVoteParticipationId(VOTE_ID, MEMBER_ID)).thenReturn(null);
        when(smallGroupMapper.selectSmallGroupVoteDetailOptions(VOTE_ID))
                .thenReturn(List.of(option(101L, "한강공원", 1, 2L)));
        when(smallGroupMapper.selectSmallGroupVoteParticipantCount(VOTE_ID)).thenReturn(2L);

        SmallGroupVoteDetailResponse response =
                smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID);

        assertThat(response.getMyParticipation().isParticipated()).isFalse();
        assertThat(response.getMyParticipation().getSelectedOptionIds()).isEmpty();
        assertThat(response.getOptions().get(0).getVoterNicknames()).isEmpty();
        verify(smallGroupMapper, never()).selectSmallGroupVoteOptionVoterNicknames(VOTE_ID);
    }

    @Test
    void getSmallGroupVoteDetailRejectsNonMembersAndInactiveMemberships() {
        for (SmallGroupMemberRelationRow relation : List.of(
                relation("MEMBER", "PENDING"),
                relation("MEMBER", "LEFT"),
                relation("MEMBER", "EXPELLED"),
                relation("MANAGER", "APPROVED"))) {
            when(smallGroupMapper.selectSmallGroupVoteListTarget(SMALL_GROUP_ID)).thenReturn(voteTarget());
            when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID)).thenReturn(List.of(relation));

            ApiException exception = catchThrowableOfType(
                    () -> smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID),
                    ApiException.class);

            assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_DETAIL_FORBIDDEN");
        }
        verify(smallGroupMapper, never()).selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void getSmallGroupVoteDetailRejectsMissingSmallGroupAndVote() {
        when(smallGroupMapper.selectSmallGroupVoteListTarget(SMALL_GROUP_ID)).thenReturn(null);

        ApiException missingGroup = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID),
                ApiException.class);
        assertThat(missingGroup.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");

        mockApprovedMember("MEMBER");
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID)).thenReturn(null);

        ApiException missingVote = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID),
                ApiException.class);
        assertThat(missingVote.getCode()).isEqualTo("SMALL_GROUP_VOTE_NOT_FOUND");
    }

    @Test
    void getSmallGroupVoteDetailReturnsClosedResultWithinOneDayAndHidesExpiredVote() {
        mockApprovedMember("MEMBER");
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE_ID);
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID))
                .thenReturn(vote(false, now.minusHours(12), null));
        when(smallGroupMapper.selectSmallGroupVoteParticipationId(VOTE_ID, MEMBER_ID)).thenReturn(null);
        when(smallGroupMapper.selectSmallGroupVoteDetailOptions(VOTE_ID)).thenReturn(List.of());
        when(smallGroupMapper.selectSmallGroupVoteParticipantCount(VOTE_ID)).thenReturn(0L);

        SmallGroupVoteDetailResponse closedResponse =
                smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID);
        assertThat(closedResponse.getStatus()).isEqualTo("CLOSED");

        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID))
                .thenReturn(vote(false, now.minusHours(25), null));

        ApiException expiredVote = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID),
                ApiException.class);
        assertThat(expiredVote.getCode()).isEqualTo("SMALL_GROUP_VOTE_NOT_FOUND");
    }

    @Test
    void getSmallGroupVoteDetailRejectsInvalidIdsAndUnauthenticatedUser() {
        ApiException invalidId = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteDetail(0L, VOTE_ID, MEMBER_ID), ApiException.class);
        ApiException unauthenticated = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID, null), ApiException.class);

        assertThat(invalidId.getCode()).isEqualTo("BAD_REQUEST");
        assertThat(unauthenticated.getCode()).isEqualTo("UNAUTHORIZED");
        verify(smallGroupMapper, never()).selectSmallGroupVoteListTarget(SMALL_GROUP_ID);
    }

    private void mockApprovedMember(String role) {
        when(smallGroupMapper.selectSmallGroupVoteListTarget(SMALL_GROUP_ID)).thenReturn(voteTarget());
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation(role, "APPROVED")));
    }

    private SmallGroupVoteListTargetRow voteTarget() {
        SmallGroupVoteListTargetRow target = new SmallGroupVoteListTargetRow();
        target.setSmallGroupId(SMALL_GROUP_ID);
        target.setGroupId(GROUP_ID);
        return target;
    }

    private SmallGroupMemberRelationRow relation(String role, String status) {
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        return relation;
    }

    private SmallGroupVoteDetailRow vote(
            boolean anonymous, LocalDateTime endsAt, LocalDateTime closedAt) {
        SmallGroupVoteDetailRow vote = new SmallGroupVoteDetailRow();
        vote.setSmallGroupVoteId(VOTE_ID);
        vote.setSmallGroupId(SMALL_GROUP_ID);
        vote.setCreatorMemberId(50L);
        vote.setCreatorNickname("모임장");
        vote.setTitle("11월 모임 장소");
        vote.setMultipleChoice(true);
        vote.setAnonymous(anonymous);
        vote.setEndsAt(endsAt);
        vote.setStatus("OPEN");
        vote.setClosedAt(closedAt);
        vote.setCreatedAt(LocalDateTime.now(KOREA_ZONE_ID).minusHours(1));
        return vote;
    }

    private SmallGroupVoteOptionDetailRow option(Long optionId, String content, int displayOrder, long voteCount) {
        SmallGroupVoteOptionDetailRow option = new SmallGroupVoteOptionDetailRow();
        option.setSmallGroupVoteOptionId(optionId);
        option.setContent(content);
        option.setDisplayOrder(displayOrder);
        option.setVoteCount(voteCount);
        return option;
    }

    private SmallGroupVoteOptionVoterNicknameRow voterNickname(Long optionId, String nickname) {
        SmallGroupVoteOptionVoterNicknameRow row = new SmallGroupVoteOptionVoterNicknameRow();
        row.setSmallGroupVoteOptionId(optionId);
        row.setVoterNickname(nickname);
        return row;
    }
}
