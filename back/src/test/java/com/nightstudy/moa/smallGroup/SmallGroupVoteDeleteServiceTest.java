package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinTargetRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: 투표 작성자·개설자·승인 OWNER의 논리 삭제 권한과 소모임·투표 소속 검증을 DB 없이 확인한다.
class SmallGroupVoteDeleteServiceTest {

    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long VOTE_ID = 30L;
    private static final long OWNER_MEMBER_ID = 40L;

    @Mock
    private SmallGroupMapper smallGroupMapper;

    @InjectMocks
    private SmallGroupServiceImpl smallGroupService;

    @Test
    void deleteSmallGroupVoteSoftDeletesOpenVoteWithoutChangingHistory() {
        mockDeletableVote("OPEN");
        when(smallGroupMapper.softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID)).thenReturn(1);

        smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID);

        verify(smallGroupMapper).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
        verify(smallGroupMapper, never()).deleteSmallGroupVoteSelections(anyLong());
    }

    @Test
    void deleteSmallGroupVoteAllowsClosedVote() {
        mockDeletableVote("CLOSED");
        when(smallGroupMapper.softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID)).thenReturn(1);

        smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID);

        verify(smallGroupMapper).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void deleteSmallGroupVoteRejectsNonCreatorOrNonOwner() {
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote("OPEN"));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, 50L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_DELETE_FORBIDDEN");
        verify(smallGroupMapper, never()).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void deleteSmallGroupVoteRejectsVoteCreatorWhoIsNotGroupCreator() {
        SmallGroupVoteDetailRow vote = vote("OPEN");
        vote.setCreatorMemberId(50L);
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, 50L), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_DELETE_FORBIDDEN");
        verify(smallGroupMapper, never()).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void deleteSmallGroupVoteRejectsMissingSmallGroupOrVoteOutsideTarget() {
        ApiException missingSmallGroup = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID), ApiException.class);
        assertThat(missingSmallGroup.getCode()).isEqualTo("SMALL_GROUP_NOT_FOUND");

        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        ApiException missingVote = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID), ApiException.class);

        assertThat(missingVote.getCode()).isEqualTo("SMALL_GROUP_VOTE_NOT_FOUND");
        verify(smallGroupMapper, never()).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void deleteSmallGroupVoteTreatsAlreadyDeletedVoteAsNotFound() {
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(null);

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_NOT_FOUND");
        verify(smallGroupMapper, never()).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    @Test
    void deleteSmallGroupVoteRejectsInactiveOwnerMembership() {
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote("OPEN"));
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, OWNER_MEMBER_ID))
                .thenReturn(relation("OWNER", "LEFT"));

        ApiException exception = catchThrowableOfType(
                () -> smallGroupService.deleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID, OWNER_MEMBER_ID), ApiException.class);

        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_VOTE_DELETE_FORBIDDEN");
        verify(smallGroupMapper, never()).softDeleteSmallGroupVote(SMALL_GROUP_ID, VOTE_ID);
    }

    private void mockDeletableVote(String status) {
        when(smallGroupMapper.selectSmallGroupJoinTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target());
        when(smallGroupMapper.selectSmallGroupVoteDetailForUpdate(SMALL_GROUP_ID, VOTE_ID)).thenReturn(vote(status));
        when(smallGroupMapper.selectSmallGroupMembershipForUpdate(GROUP_ID, OWNER_MEMBER_ID))
                .thenReturn(relation("OWNER", "APPROVED"));
    }

    private SmallGroupJoinTargetRow target() {
        SmallGroupJoinTargetRow target = new SmallGroupJoinTargetRow();
        target.setSmallGroupId(SMALL_GROUP_ID);
        target.setGroupId(GROUP_ID);
        target.setCreatorMemberId(OWNER_MEMBER_ID);
        return target;
    }

    private SmallGroupVoteDetailRow vote(String status) {
        SmallGroupVoteDetailRow vote = new SmallGroupVoteDetailRow();
        vote.setSmallGroupVoteId(VOTE_ID);
        vote.setSmallGroupId(SMALL_GROUP_ID);
        vote.setCreatorMemberId(OWNER_MEMBER_ID);
        vote.setStatus(status);
        return vote;
    }

    private SmallGroupMemberRelationRow relation(String role, String status) {
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        return relation;
    }
}
