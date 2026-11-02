package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 실제 신청과 상세 응답의 제한 일치 및 개인정보 없는 본인 상태 응답을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupJoinAvailabilityServiceTest {
    @Mock private SavingGroupMapper mapper;
    @InjectMocks private SavingGroupServiceImpl service;
    private SavingGroupRow row;
    private SavingGroupJoinMemberRow member;

    @BeforeEach
    void setup() {
        row = new SavingGroupRow();
        row.setSavingGroupId(10L);
        row.setGroupId(20L);
        row.setMaxMembers(10);
        row.setStatus("01");
        member = new SavingGroupJoinMemberRow();
        member.setMemberId(30L);
        member.setAccountStatus("ACTIVE");
        member.setIdentityVerifiedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        
        // AI작성: 제한 우선순위에 따라 일부 조회는 생략되므로 공통 기본값만 lenient로 설정한다.
        lenient().when(mapper.selectSavingGroupDetail(20L)).thenReturn(row);
        lenient().when(mapper.selectDetailMemberCount(20L)).thenReturn(2L);
        lenient().when(mapper.selectSavingGroupJoinMember(30L)).thenReturn(member);
    }

    @Test
    void anonymousDetailRemainsPublicButCannotApply() {
        SavingGroupDetailResponse response = service.getSavingGroupDetail(20L, null);
        assertThat(response.isCanApply()).isFalse();
        assertThat(response.getMembershipStatus()).isNull();
        assertThat(response.getJoinRestrictionCode()).isEqualTo("UNAUTHORIZED");
        verify(mapper, never()).selectSavingGroupJoinMember(any());
    }

    @Test
    void newMemberCanApplyWithoutRestriction() {
        SavingGroupDetailResponse response = detail();
        assertThat(response.isCanApply()).isTrue();
        assertThat(response.getMembershipStatus()).isNull();
        assertThat(response.getJoinRestrictionCode()).isNull();
        assertThat(response.getJoinRestrictionMessage()).isNull();
    }

    @Test
    void existingMembershipRestrictionsTakePriorityOverFullCapacity() {
        when(mapper.selectDetailMemberCount(20L)).thenReturn(10L);
        for (String status : List.of("APPROVED", "PENDING", "REJECTED", "EXPELLED")) {
            setRelation(status);
            SavingGroupDetailResponse response = detail();
            assertThat(response.getMembershipStatus()).isEqualTo(status);
            assertThat(response.isCanApply()).isFalse();
            assertThat(response.getJoinRestrictionCode()).isEqualTo(switch (status) {
                case "APPROVED" -> "ALREADY_JOINED_SAVING_GROUP";
                case "PENDING" -> "JOIN_REQUEST_ALREADY_PENDING";
                default -> "JOIN_REAPPLICATION_FORBIDDEN";
            });
            if (status.equals("REJECTED")) assertThat(response.getJoinRestrictionMessage()).contains("거절");
            if (status.equals("EXPELLED")) assertThat(response.getJoinRestrictionMessage()).contains("추방");
        }
    }

    @Test
    void voluntaryLeaverCanApplyAgain() {
        setRelation("LEFT");
        assertThat(detail().isCanApply()).isTrue();
        assertThat(detail().getMembershipStatus()).isEqualTo("LEFT");
    }

    @Test
    void invalidMembershipDataIsNotHiddenAsNormalRestriction() {
        setRelation("UNKNOWN");
        ApiException error = catchThrowableOfType(() -> detail(), ApiException.class);
        assertThat(error.getCode()).isEqualTo("MEMBERSHIP_STATUS_INVALID");
    }

    @Test
    void fullOrMissingCapacityCannotApply() {
        row.setMaxMembers(2);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SAVING_GROUP_CAPACITY_FULL");
        row.setMaxMembers(null);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SAVING_GROUP_CAPACITY_NOT_CONFIGURED");
    }

    @Test
    void unavailableOrUnverifiedAccountCannotApply() {
        member.setAccountStatus("SUSPENDED");
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("MEMBER_ACCOUNT_UNAVAILABLE");
        member.setAccountStatus("ACTIVE");
        member.setIdentityVerifiedAt(null);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("IDENTITY_VERIFICATION_REQUIRED");
    }

    @Test
    void closedRecruitmentAndFiveJoinedGroupsCannotApply() {
        row.setStatus("02");
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SAVING_GROUP_JOIN_UNAVAILABLE");
        row.setStatus("01");
        when(mapper.selectApprovedSavingGroupCountReadOnly(30L)).thenReturn(5L);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SAVING_GROUP_JOIN_LIMIT_EXCEEDED");
    }

    // AI작성: UI를 거치지 않은 직접 요청도 상세 응답과 같은 가입 상태 제한을 적용한다.
    @Test
    void directRequestRejectsExistingMembershipAndReactivatesOnlyLeft() {
        SavingGroupJoinTargetRow target = new SavingGroupJoinTargetRow();
        target.setSavingGroupId(10L);
        target.setGroupId(20L);
        target.setStatus("01");
        target.setMaxMembers(10);
        when(mapper.selectSavingGroupJoinMemberForUpdate(30L)).thenReturn(member);
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(target);
        for (String status : List.of("APPROVED", "PENDING", "REJECTED", "EXPELLED")) {
            setRelation(status);
            SavingGroupMembershipRelationRow relation = new SavingGroupMembershipRelationRow();
            relation.setMembershipStatus(status);
            when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation);
            String expectedCode = detail().getJoinRestrictionCode();
            ApiException error = catchThrowableOfType(() -> service.requestSavingGroupJoin(10L, 30L), ApiException.class);
            assertThat(error.getCode()).isEqualTo(expectedCode);
        }
        setRelation("LEFT");
        SavingGroupMembershipRelationRow left = new SavingGroupMembershipRelationRow();
        left.setMembershipStatus("LEFT");
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(left);
        when(mapper.reactivateLeftSavingGroupMembership(20L, 30L)).thenReturn(1);
        assertThat(detail().isCanApply()).isTrue();
        service.requestSavingGroupJoin(10L, 30L);
        verify(mapper).reactivateLeftSavingGroupMembership(20L, 30L);
        verify(mapper, never()).insertSavingGroupJoinRequest(any(), any());
    }

    // AI작성: 실제 MyBatis 매핑을 파싱해 상세 자격 조회가 읽기 전용이며 일정 조회가 포함되는지 검증한다.
    @Test
    void mapperSupportsReadOnlyEligibilityQueries() throws Exception {
        String resource = "mappers/SavingGroupMapper.xml";
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.getTypeAliasRegistry().registerAlias(SavingGroupCreationRequest.class);
        try (java.io.Reader reader = org.apache.ibatis.io.Resources.getResourceAsReader(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        String sql = configuration.getMappedStatement(namespace + "selectSavingGroupJoinMember")
                .getBoundSql(java.util.Map.of("memberId", 30L)).getSql();
        assertThat(sql.toUpperCase(java.util.Locale.ROOT)).doesNotContain("FOR UPDATE");
        for (String id : List.of("selectSavingGroupMembership", "selectApprovedSavingGroupCountReadOnly")) {
            assertThat(configuration.getMappedStatement(namespace + id)
                    .getBoundSql(java.util.Map.of("memberId", 30L, "groupId", 20L)).getSql().toUpperCase(java.util.Locale.ROOT))
                    .doesNotContain("FOR UPDATE");
        }
    }

    private SavingGroupDetailResponse detail() { return service.getSavingGroupDetail(20L, 30L); }

    private void setRelation(String status) {
        SavingGroupMembershipRelationRow relation = new SavingGroupMembershipRelationRow();
        relation.setMemberRole("MEMBER");
        relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(relation);
    }
}
