package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteParticipationRequest;
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
// AI작성: 최초 참여·동일 선택 재요청·선택 변경과 참여 완료 회원 전용 현황 조회 정책을 DB 없이 검증한다.
class SmallGroupVoteParticipationServiceTest {

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
    void participateSmallGroupVoteCreatesParticipationAndSelections() {
        mockParticipatableVote(false);
        when(smallGroupMapper.selectSmallGroupVoteOptionIds(VOTE_ID)).thenReturn(List.of(101L, 102L));
        when(smallGroupMapper.selectSmallGroupVoteParticipationIdForUpdate(VOTE_ID, MEMBER_ID))
                .thenReturn(null, 90L);
        when(smallGroupMapper.insertSmallGroupVoteParticipation(VOTE_ID, MEMBER_ID)).thenReturn(1);
        when(smallGroupMapper.insertSmallGroupVoteSelections(90L, List.of(101L))).thenReturn(1);

        smallGroupService.participateSmallGroupVote(
                SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(101L));

        verify(smallGroupMapper).insertSmallGroupVoteParticipation(VOTE_ID, MEMBER_ID);
        verify(smallGroupMapper).insertSmallGroupVoteSelections(90L, List.of(101L));
    }

    @Test
    void participateSmallGroupVoteLeavesSameSelectionUntouched() {
        mockParticipatableVote(true);
        when(smallGroupMapper.selectSmallGroupVoteOptionIds(VOTE_ID)).thenReturn(List.of(101L, 102L));
        when(smallGroupMapper.selectSmallGroupVoteParticipationIdForUpdate(VOTE_ID, MEMBER_ID)).thenReturn(90L);
        when(smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(90L)).thenReturn(List.of(101L));

        smallGroupService.participateSmallGroupVote(
                SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(101L));

        verify(smallGroupMapper, never()).deleteSmallGroupVoteSelections(90L);
        verify(smallGroupMapper, never()).updateSmallGroupVoteParticipationTime(90L);
    }

    @Test
    void participateSmallGroupVoteReplacesChangedSelectionsAndUpdatesTime() {
        mockParticipatableVote(true);
        when(smallGroupMapper.selectSmallGroupVoteOptionIds(VOTE_ID)).thenReturn(List.of(101L, 102L));
        when(smallGroupMapper.selectSmallGroupVoteParticipationIdForUpdate(VOTE_ID, MEMBER_ID)).thenReturn(90L);
        when(smallGroupMapper.selectSmallGroupVoteSelectedOptionIds(90L)).thenReturn(List.of(101L));
        when(smallGroupMapper.deleteSmallGroupVoteSelections(90L)).thenReturn(1);
        when(smallGroupMapper.insertSmallGroupVoteSelections(90L, List.of(102L))).thenReturn(1);
        when(smallGroupMapper.updateSmallGroupVoteParticipationTime(90L)).thenReturn(1);

        smallGroupService.participateSmallGroupVote(
                SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(102L));

        verify(smallGroupMapper).deleteSmallGroupVoteSelections(90L);
        verify(smallGroupMapper).insertSmallGroupVoteSelections(90L, List.of(102L));
        verify(smallGroupMapper).updateSmallGroupVoteParticipationTime(90L);
    }

    @Test
    void participateSmallGroupVoteRejectsSingleChoiceMultipleSelectionAndForeignOption() {
        mockParticipatableVote(false);

        ApiException singleChoiceException = catchThrowableOfType(
                () -> smallGroupService.participateSmallGroupVote(
                        SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(101L, 102L)), ApiException.class);

        assertThat(singleChoiceException.getCode()).isEqualTo("SMALL_GROUP_VOTE_SINGLE_CHOICE_REQUIRED");

        mockParticipatableVote(true);
        when(smallGroupMapper.selectSmallGroupVoteOptionIds(VOTE_ID)).thenReturn(List.of(101L));
        ApiException foreignOptionException = catchThrowableOfType(
                () -> smallGroupService.participateSmallGroupVote(
                        SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(102L)), ApiException.class);

        assertThat(foreignOptionException.getCode()).isEqualTo("SMALL_GROUP_VOTE_OPTION_INVALID");
    }

    @Test
    void participateSmallGroupVoteRejectsClosedVoteAndInactiveMembership() {
        mockParticipatableVote(true);
        SmallGroupVoteDetailRow closedVote = vote(true);
        closedVote.setStatus("CLOSED");
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(closedVote);

        ApiException closedException = catchThrowableOfType(
                () -> smallGroupService.participateSmallGroupVote(
                        SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(101L)), ApiException.class);
        assertThat(closedException.getCode()).isEqualTo("SMALL_GROUP_VOTE_CLOSED");

        SmallGroupMemberRelationRow leftMember = relation("MEMBER", "LEFT");
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID)).thenReturn(leftMember);
        ApiException membershipException = catchThrowableOfType(
                () -> smallGroupService.participateSmallGroupVote(
                        SMALL_GROUP_ID, VOTE_ID, MEMBER_ID, request(101L)), ApiException.class);
        assertThat(membershipException.getCode()).isEqualTo("SMALL_GROUP_VOTE_PARTICIPATION_FORBIDDEN");
    }

    @Test
    void getSmallGroupVoteStatusRejectsNotParticipatedMember() {
        when(smallGroupMapper.selectSmallGroupVoteListTarget(SMALL_GROUP_ID)).thenReturn(voteTarget());
        when(smallGroupMapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("MEMBER", "APPROVED")));
        when(smallGroupMapper.selectSmallGroupVoteDetail(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote(true));
        when(smallGroupMapper.selectSmallGroupVoteParticipationId(VOTE_ID, MEMBER_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.getSmallGroupVoteStatus(SMALL_GROUP_ID, VOTE_ID, MEMBER_ID),
                ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_STATUS_FORBIDDEN");
        verify(smallGroupMapper, never()).selectSmallGroupVoteDetailOptions(VOTE_ID);
    }

    private void mockParticipatableVote(boolean multipleChoice) {
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(joinTarget());
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID))
                .thenReturn(relation("MEMBER", "APPROVED"));
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID))
                .thenReturn(vote(multipleChoice));
    }

    private SmallGroupVoteParticipationRequest request(Long... selectedOptionIds) {
        SmallGroupVoteParticipationRequest request = new SmallGroupVoteParticipationRequest();
        request.setSelectedOptionIds(List.of(selectedOptionIds));
        return request;
    }

    private SmallGroupJoinTargetRow joinTarget() {
        SmallGroupJoinTargetRow target = new SmallGroupJoinTargetRow();
        target.setSmallGroupId(SMALL_GROUP_ID);
        target.setGroupId(GROUP_ID);
        return target;
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

    private SmallGroupVoteDetailRow vote(boolean multipleChoice) {
        SmallGroupVoteDetailRow vote = new SmallGroupVoteDetailRow();
        vote.setSmallGroupVoteId(VOTE_ID);
        vote.setSmallGroupId(SMALL_GROUP_ID);
        vote.setMultipleChoice(multipleChoice);
        vote.setAnonymous(false);
        vote.setStatus("OPEN");
        vote.setEndsAt(LocalDateTime.now(KOREA_ZONE_ID).plusHours(1));
        return vote;
    }
}
