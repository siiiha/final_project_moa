package com.nightstudy.moa.savingGroup.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinMemberRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupJoinTargetRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupListCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMemberCount;
import com.nightstudy.moa.savingGroup.dto.SavingGroupMembershipRelationRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupRow;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SavingGroupServiceImplTest {

    @Test
    void 정상회원은정원과가입수여유가있으면대기신청을저장한다() {
        FakeSavingGroupMapper mapper = new FakeSavingGroupMapper();
        mapper.approvedMemberCount = 2;
        mapper.approvedSavingGroupCount = 4;
        SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper);

        assertDoesNotThrow(() -> service.requestSavingGroupJoin(10L, 20L));

        assertEquals(1, mapper.insertCalls);
        assertEquals(0, mapper.reactivateCalls);
    }

    @Test
    void 대기중인회원은중복신청오류를반환한다() {
        FakeSavingGroupMapper mapper = new FakeSavingGroupMapper();
        mapper.relation = relation("PENDING");
        SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.requestSavingGroupJoin(10L, 20L));

        assertEquals("JOIN_REQUEST_ALREADY_PENDING", exception.getCode());
        assertEquals(0, mapper.insertCalls);
    }

    @Test
    void 자진탈퇴회원은새행대신기존관계를대기상태로갱신한다() {
        FakeSavingGroupMapper mapper = new FakeSavingGroupMapper();
        mapper.relation = relation("LEFT");
        SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper);

        assertDoesNotThrow(() -> service.requestSavingGroupJoin(10L, 20L));

        assertEquals(0, mapper.insertCalls);
        assertEquals(1, mapper.reactivateCalls);
    }

    private static SavingGroupMembershipRelationRow relation(String membershipStatus) {
        SavingGroupMembershipRelationRow relation = new SavingGroupMembershipRelationRow();
        relation.setMembershipStatus(membershipStatus);
        return relation;
    }

    private static class FakeSavingGroupMapper implements SavingGroupMapper {
        private final SavingGroupJoinMemberRow member = activeVerifiedMember();
        private final SavingGroupJoinTargetRow target = openSavingGroup();
        private SavingGroupMembershipRelationRow relation;
        private long approvedMemberCount;
        private long approvedSavingGroupCount;
        private int insertCalls;
        private int reactivateCalls;

        @Override
        public int insertGroups(SavingGroupCreationRequest request) {
            return 0;
        }

        @Override
        public int insertSavingGroup(SavingGroupCreationRequest request) {
            return 0;
        }

        @Override
        public int insertSavingGroupCreationApproval(Long groupId) {
            return 0;
        }

        @Override
        public Long selectRegionIdByName(String regionName) {
            return null;
        }

        @Override
        public boolean existsMember(Long memberId) {
            return false;
        }

        @Override
        public int selectSavingGroupCreationApplicationCount(SavingGroupCreationApplicationListCondition condition) {
            return 0;
        }

        @Override
        public List<SavingGroupCreationApplicationListItemResponse> selectSavingGroupCreationApplications(
                SavingGroupCreationApplicationListCondition condition) {
            return List.of();
        }

        @Override
        public SavingGroupCreationApplicationDetailResponse selectSavingGroupCreationApplication(Long approvalId) {
            return null;
        }

        @Override
        public SavingGroupCreationApplicationDetailResponse selectSavingGroupCreationApplicationForUpdate(Long approvalId) {
            return null;
        }

        @Override
        public int approveSavingGroupCreationApplication(Long approvalId) {
            return 0;
        }

        @Override
        public int rejectSavingGroupCreationApplication(Long approvalId, String rejectReason) {
            return 0;
        }

        @Override
        public int publishApprovedSavingGroup(Long groupId) {
            return 0;
        }

        @Override
        public int selectSavingGroupJoinRequestCount(Long groupId, String status) {
            return 0;
        }

        @Override
        public List<SavingGroupJoinRequestRow> selectSavingGroupJoinRequests(
                Long groupId, String status, int offset, int limit) {
            return List.of();
        }

        @Override
        public int approveSavingGroupJoinRequest(Long crewMemberId, Long groupId, Long approvedBy) {
            return 0;
        }

        @Override
        public SavingGroupJoinApprovalTargetRow selectSavingGroupJoinApprovalTarget(Long crewMemberId) {
            return null;
        }

        @Override
        public SavingGroupJoinApprovalTargetRow selectSavingGroupJoinApprovalTargetForUpdate(Long crewMemberId) {
            return null;
        }

        @Override
        public int selectSavingGroupCount(SavingGroupListCondition condition) {
            return 0;
        }

        @Override
        public List<SavingGroupRow> selectSavingGroupList(SavingGroupListCondition condition) {
            return List.of();
        }

        @Override
        public List<SavingGroupMemberCount> selectApprovedMemberCounts(List<Long> groupIds) {
            return List.of();
        }

        @Override
        public SavingGroupRow selectSavingGroupDetail(Long groupId) {
            return null;
        }

        @Override
        public long selectDetailMemberCount(Long groupId) {
            return 0;
        }

        @Override
        public List<SavingGroupNoticeSummaryResponse> selectRecentNotices(Long groupId) {
            return List.of();
        }

        @Override
        public List<String> selectApprovedMemberRoles(Long groupId, Long memberId) {
            return List.of();
        }

        @Override
        public SavingGroupSettingsAuthorityRow selectSavingGroupSettingsAuthority(Long savingGroupId) {
            return null;
        }

        @Override
        public SavingGroupCurrentSettingsResponse selectSavingGroupCurrentSettings(Long savingGroupId) {
            return null;
        }

        @Override
        public SavingGroupSettingsAuthorityRow selectSavingGroupSettingsAuthorityForUpdate(Long savingGroupId) {
            return null;
        }

        @Override
        public Long selectSavingGroupOwnerMemberIdForUpdate(Long groupId) {
            return null;
        }

        @Override
        public long selectApprovedMemberCountForUpdate(Long groupId) {
            return approvedMemberCount;
        }

        @Override
        public int updateSavingGroupBasicSettings(SavingGroupSettingsBasicUpdate update) {
            return 0;
        }

        @Override
        public int updateSavingGroupCurrentDues(SavingGroupCurrentDuesUpdate update) {
            return 0;
        }

        @Override
        public int upsertSavingGroupSettingChange(SavingGroupSettingChangeRow change) {
            return 0;
        }

        @Override
        public List<SavingGroupSettingChangeRow> selectDueSettingChanges(LocalDateTime appliedAt) {
            return List.of();
        }

        @Override
        public String selectSavingGroupStatusForUpdate(Long groupId) {
            return null;
        }

        @Override
        public SavingGroupSettingChangeRow selectSavingGroupSettingChangeForApply(
                Long settingChangeId, LocalDateTime appliedAt) {
            return null;
        }

        @Override
        public int applySavingGroupSettingChange(SavingGroupSettingChangeRow change) {
            return 0;
        }

        @Override
        public int markSavingGroupSettingChangeApplied(Long settingChangeId, LocalDateTime appliedAt) {
            return 0;
        }

        @Override
        public int updateSavingGroupStatus(Long groupId, String status) {
            return 0;
        }

        @Override
        public int holdPendingSavingGroupSettingChanges(Long groupId) {
            return 0;
        }

        @Override
        public int cancelUnappliedSavingGroupSettingChanges(Long groupId, LocalDateTime canceledAt) {
            return 0;
        }

        @Override
        public int resumeHeldSavingGroupSettingChanges(
                Long groupId, LocalDateTime resumedAt, LocalDateTime nextEffectiveAt) {
            return 0;
        }

        @Override
        public SavingGroupJoinMemberRow selectSavingGroupJoinMemberForUpdate(Long memberId) {
            return member;
        }

        @Override
        public SavingGroupJoinTargetRow selectSavingGroupJoinTargetForUpdate(Long savingGroupId) {
            return target;
        }

        @Override
        public SavingGroupMembershipRelationRow selectSavingGroupMembershipForUpdate(Long groupId, Long memberId) {
            return relation;
        }

        @Override
        public long selectApprovedMemberCountForUpdate(Long groupId) {
            return approvedMemberCount;
        }

        @Override
        public long selectApprovedSavingGroupCount(Long memberId) {
            return approvedSavingGroupCount;
        }

        @Override
        public int insertSavingGroupJoinRequest(Long groupId, Long memberId) {
            insertCalls++;
            return 1;
        }

        @Override
        public int reactivateLeftSavingGroupMembership(Long groupId, Long memberId) {
            reactivateCalls++;
            return 1;
        }

        private static SavingGroupJoinMemberRow activeVerifiedMember() {
            SavingGroupJoinMemberRow member = new SavingGroupJoinMemberRow();
            member.setMemberId(20L);
            member.setAccountStatus("ACTIVE");
            member.setIdentityVerifiedAt(LocalDateTime.now());
            return member;
        }

        private static SavingGroupJoinTargetRow openSavingGroup() {
            SavingGroupJoinTargetRow target = new SavingGroupJoinTargetRow();
            target.setSavingGroupId(10L);
            target.setGroupId(30L);
            target.setStatus("01");
            target.setMaxMembers(5);
            return target;
        }
    }
}
