package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.*;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
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
class SmallGroupJoinAvailabilityServiceTest {
    @Mock private SmallGroupMapper mapper;
    @InjectMocks private SmallGroupServiceImpl service;
    private SmallGroupDetailRow row;
    private SmallGroupJoinMemberRow member;

    @BeforeEach
    void setup() {
        row = new SmallGroupDetailRow();
        row.setSmallGroupId(10L);
        row.setGroupId(20L);
        row.setMaxMembers(10);
        row.setMeetDate(today().plusDays(1));
        row.setCreatorMemberId(40L);
        member = new SmallGroupJoinMemberRow();
        member.setMemberId(30L);
        member.setAccountStatus("ACTIVE");
        member.setIdentityVerifiedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        member.setSystemRole("USER");
        // AI작성: 제한 우선순위에 따라 일부 조회는 생략되므로 공통 기본값만 lenient로 설정한다.
        lenient().when(mapper.selectSmallGroupDetail(10L)).thenReturn(row);
        lenient().when(mapper.selectApprovedSmallGroupMemberCount(20L)).thenReturn(2L);
        lenient().when(mapper.selectSmallGroupJoinMember(30L)).thenReturn(member);
    }

    @Test
    void anonymousDetailRemainsPublicButCannotApply() {
        SmallGroupDetailResponse response = service.getSmallGroupDetail(10L, null);
        assertThat(response.isCanApply()).isFalse();
        assertThat(response.getMembershipStatus()).isNull();
        assertThat(response.getJoinRestrictionCode()).isEqualTo("UNAUTHORIZED");
        verify(mapper, never()).selectSmallGroupJoinMember(any());
    }

    @Test
    void newMemberCanApplyWithoutRestriction() {
        SmallGroupDetailResponse response = detail();
        assertThat(response.isCanApply()).isTrue();
        assertThat(response.getMembershipStatus()).isNull();
        assertThat(response.getJoinRestrictionCode()).isNull();
        assertThat(response.getJoinRestrictionMessage()).isNull();
    }

    @Test
    void existingMembershipRestrictionsTakePriorityOverFullCapacity() {
        when(mapper.selectApprovedSmallGroupMemberCount(20L)).thenReturn(10L);
        for (String status : List.of("APPROVED", "PENDING", "REJECTED", "EXPELLED")) {
            setRelation(status);
            SmallGroupDetailResponse response = detail();
            assertThat(response.getMembershipStatus()).isEqualTo(status);
            assertThat(response.isCanApply()).isFalse();
            assertThat(response.getJoinRestrictionCode()).isEqualTo(switch (status) {
                case "APPROVED" -> "ALREADY_JOINED_SMALL_GROUP";
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
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SMALL_GROUP_CAPACITY_FULL");
        row.setMaxMembers(null);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SMALL_GROUP_CAPACITY_NOT_CONFIGURED");
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
    void meetingTodayAndFutureAllowApplication() {
        row.setMeetDate(today());
        assertThat(detail().isCanApply()).isTrue();
        row.setMeetDate(today().plusDays(1));
        assertThat(detail().isCanApply()).isTrue();
    }

    @Test
    void elapsedOrMissingMeetingCannotApplyInDetailOrDirectRequest() {
        SmallGroupJoinTargetRow target = new SmallGroupJoinTargetRow();
        target.setSmallGroupId(10L);
        target.setGroupId(20L);
        target.setCreatorMemberId(40L);
        target.setMaxMembers(10);
        when(mapper.selectSmallGroupJoinMemberForUpdate(30L)).thenReturn(member);
        when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(target);
        for (LocalDate date : new LocalDate[] { today().minusDays(1), null }) {
            row.setMeetDate(date);
            target.setMeetDate(date);
            String code = date == null ? "SMALL_GROUP_DATE_NOT_CONFIGURED" : "SMALL_GROUP_MEETING_ENDED";
            assertThat(detail().getJoinRestrictionCode()).isEqualTo(code);
            ApiException error = catchThrowableOfType(() -> service.requestSmallGroupJoin(10L, 30L), ApiException.class);
            assertThat(error.getCode()).isEqualTo(code);
        }
        verify(mapper, never()).insertSmallGroupJoinRequest(any(), any());
        verify(mapper, never()).reactivateLeftSmallGroupMembership(any(), any());
    }

    @Test
    void systemAdminAndCreatorCannotApply() {
        member.setSystemRole("ADMIN");
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SMALL_GROUP_JOIN_ADMIN_NOT_ALLOWED");
        member.setSystemRole("USER");
        row.setCreatorMemberId(30L);
        assertThat(detail().getJoinRestrictionCode()).isEqualTo("SMALL_GROUP_OPERATOR_JOIN_NOT_ALLOWED");
    }

    // AI작성: 상세 안내뿐 아니라 일정 당일의 직접 가입 신청도 정상 저장한다.
    @Test
    void directRequestAllowsMeetingToday() {
        row.setMeetDate(today());
        assertThat(detail().isCanApply()).isTrue();
        SmallGroupJoinTargetRow target = new SmallGroupJoinTargetRow();
        target.setSmallGroupId(10L);
        target.setGroupId(20L);
        target.setCreatorMemberId(40L);
        target.setMaxMembers(10);
        target.setMeetDate(today());
        when(mapper.selectSmallGroupJoinMemberForUpdate(30L)).thenReturn(member);
        when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(target);
        when(mapper.insertSmallGroupJoinRequest(20L, 30L)).thenReturn(1);
        service.requestSmallGroupJoin(10L, 30L);
        verify(mapper).insertSmallGroupJoinRequest(20L, 30L);
    }

    private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Seoul")); }

    // AI작성: 실제 MyBatis 매핑을 파싱해 상세 자격 조회가 읽기 전용이며 일정 조회가 포함되는지 검증한다.
    @Test
    void mapperSupportsReadOnlyEligibilityQueries() throws Exception {
        String resource = "mappers/SmallGroupMapper.xml";
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        try (java.io.Reader reader = org.apache.ibatis.io.Resources.getResourceAsReader(resource)) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = "com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper.";
        String sql = configuration.getMappedStatement(namespace + "selectSmallGroupJoinMember")
                .getBoundSql(java.util.Map.of("memberId", 30L)).getSql();
        assertThat(sql.toUpperCase(java.util.Locale.ROOT)).doesNotContain("FOR UPDATE");
        assertThat(configuration.getMappedStatement(namespace + "selectSmallGroupJoinTargetForUpdate")
                .getBoundSql(java.util.Map.of("smallGroupId", 10L)).getSql()).contains("sg.meet_date", "FOR UPDATE");
        assertThat(configuration.getMappedStatement(namespace + "selectSmallGroupDetail")
                .getBoundSql(java.util.Map.of("smallGroupId", 10L)).getSql()).contains("creator_member_id");
    }

    private SmallGroupDetailResponse detail() { return service.getSmallGroupDetail(10L, 30L); }

    private void setRelation(String status) {
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole("MEMBER");
        relation.setMembershipStatus(status);
        when(mapper.selectSmallGroupMemberRelations(20L, 30L)).thenReturn(List.of(relation));
    }
}
