package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 고정된 서울 날짜로 OWNER 포함 전체 배정과 다음 달 예약을 검증한다.
class SavingGroupTurnOrderAssignmentServiceTest {
    SavingGroupMapper mapper;
    SavingGroupTurnOrderServiceImpl service;
    SavingGroupTurnOrderPlanRow group;
    @BeforeEach void setup() {
        mapper = mock(SavingGroupMapper.class);
        service = new SavingGroupTurnOrderServiceImpl(mapper, Clock.fixed(Instant.parse("2026-11-06T00:00:00Z"), ZoneId.of("Asia/Seoul")));
        group = new SavingGroupTurnOrderPlanRow(); group.setSavingGroupId(10L); group.setGroupId(20L);
        group.setGroupType("순번계"); group.setStatus("01"); group.setStartDate(LocalDate.of(2026, 10, 1));
        when(mapper.selectTurnOrderGroupForUpdate(10L)).thenReturn(group);
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(owner); when(mapper.existsMember(30L)).thenReturn(true);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(List.of(member("30", false), member("40", false)));
        when(mapper.updateTurnOrderSequence(anyLong(), anyLong(), anyInt())).thenReturn(1);
    }
    static SavingGroupTurnOrderAssignmentRow member(String id, boolean paid) {
        var row = new SavingGroupTurnOrderAssignmentRow(); row.setMemberId(id); row.setNickname("테스트회원"); row.setPaid(paid); return row;
    }
    static SavingGroupTurnOrderUpdateRequest request(long a, int x, long b, int y) {
        return new SavingGroupTurnOrderUpdateRequest(List.of(new SavingGroupTurnOrderUpdateRequest.Entry(a, x), new SavingGroupTurnOrderUpdateRequest.Entry(b, y)));
    }
    @Test void assignsOwnerAndEveryEligibleMemberOnce() {
        service.update(10L, 30L, request(30, 1, 40, 2));
        verify(mapper).updateTurnOrderSequence(20L, 30L, 1); verify(mapper).updateTurnOrderSequence(20L, 40L, 2);
        verify(mapper, never()).insertTurnOrderPlan(any());
    }
    @Test void schedulesNextMonthAndReplacesWholePlanWithoutChangingCurrentOrder() {
        group.setStatus("02");
        var current = member("50", false);
        current.setSequenceNumber(2);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(
                List.of(member("30", false), member("40", false), current));
        doAnswer(call -> { ((SavingGroupTurnOrderPlanRow) call.getArgument(0)).setTurnOrderPlanId(99L); return 1; }).when(mapper).insertTurnOrderPlan(any());
        when(mapper.insertTurnOrderPlanMember(anyLong(), anyLong(), anyInt())).thenReturn(1);
        service.update(10L, 30L, request(30, 3, 40, 4));
        verify(mapper).insertTurnOrderPlan(argThat(p -> p.getEffectiveMonth().equals(LocalDate.of(2026,12,1)) && p.getStartRoundNumber() == 3));
        var order = inOrder(mapper); order.verify(mapper).deleteTurnOrderPlanMembers(99L);
        order.verify(mapper).insertTurnOrderPlanMember(99L, 30L, 3); order.verify(mapper).insertTurnOrderPlanMember(99L, 40L, 4);
        verify(mapper, never()).updateTurnOrderSequence(anyLong(), anyLong(), anyInt());
    }
    // AI작성: 4인 순번계의 3회차에서는 남은 한 명의 순번을 변경할 수 없다.
    @Test void rejectsReassignmentWhenOnlyOneMemberRemains() {
        group.setStatus("02");
        group.setStartDate(LocalDate.of(2026, 9, 1));
        var current = member("50", false);
        current.setSequenceNumber(3);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(
                List.of(member("30", true), member("40", true), current, member("60", false)));
        assertThatThrownBy(() -> service.update(10L, 30L,
                new SavingGroupTurnOrderUpdateRequest(List.of(new SavingGroupTurnOrderUpdateRequest.Entry(60L, 4)))))
                .isInstanceOf(ApiException.class).hasMessage("변경할 수 있는 순번이 없습니다.");
        verify(mapper, never()).insertTurnOrderPlan(any());
    }
    @ParameterizedTest @ValueSource(strings = {"MANAGER", "MEMBER"})
    void rejectsNonOwnerRoles(String role) {
        var relation = new SavingGroupMembershipRelationRow(); relation.setMemberRole(role); relation.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembershipForUpdate(20L,30L)).thenReturn(relation);
        assertThatThrownBy(() -> service.update(10L,30L,request(30,1,40,2))).isInstanceOf(ApiException.class);
        verify(mapper,never()).updateTurnOrderSequence(anyLong(),anyLong(),anyInt());
    }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void rejectsInactiveOwner(String status) {
        var relation = new SavingGroupMembershipRelationRow(); relation.setMemberRole("OWNER"); relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembershipForUpdate(20L,30L)).thenReturn(relation);
        assertThatThrownBy(() -> service.get(10L,30L)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsAnonymousNonMemberAndOtherGroupOwner() {
        assertThatThrownBy(() -> service.get(10L,null)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.get(10L,99L)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.get(10L,88L)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsMissingAndNonTurnGroup() {
        assertThatThrownBy(() -> service.get(99L,30L)).isInstanceOf(ApiException.class);
        group.setGroupType("친목계"); assertThatThrownBy(() -> service.get(10L,30L)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsDuplicatesMissingForeignAndUnapprovedMembersAndInvalidRoundsBeforeWriting() {
        for (var request : List.of(request(30,1,30,2), request(30,1,40,1), request(30,1,99,2), request(30,0,40,2), request(30,1,40,3),
                new SavingGroupTurnOrderUpdateRequest(List.of(new SavingGroupTurnOrderUpdateRequest.Entry(30L,1))))) {
            assertThatThrownBy(() -> service.update(10L,30L,request)).isInstanceOf(ApiException.class);
        }
        verify(mapper,never()).updateTurnOrderSequence(anyLong(),anyLong(),anyInt());
    }
    @Test void excludesCompletedOwnerButOwnerCanStillAssign() {
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(List.of(member("30",true),member("40",false)));
        assertThatThrownBy(() -> service.update(10L,30L,request(30,1,40,2))).isInstanceOf(ApiException.class);
        service.update(10L,30L,new SavingGroupTurnOrderUpdateRequest(List.of(new SavingGroupTurnOrderUpdateRequest.Entry(40L,1))));
        verify(mapper).updateTurnOrderSequence(20L,40L,1); verify(mapper,never()).updateTurnOrderSequence(20L,30L,1);
    }
    @Test void rejectsPeriodOverflowAndFailedPartialUpdate() {
        group.setStatus("02"); group.setEndDate(LocalDate.of(2026,12,31));
        assertThatThrownBy(() -> service.update(10L,30L,request(30,3,40,4))).isInstanceOf(ApiException.class);
        group.setStatus("01"); when(mapper.updateTurnOrderSequence(20L,40L,2)).thenReturn(0);
        assertThatThrownBy(() -> service.update(10L,30L,request(30,1,40,2))).isInstanceOf(ApiException.class);
    }
    @Test void repeatedIdenticalAssignmentDoesNotDependOnChangedRowsSetting() {
        var owner=member("30",false); owner.setSequenceNumber(1);
        var member=member("40",false); member.setSequenceNumber(2);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(List.of(owner,member));
        service.update(10L,30L,request(30,1,40,2));
        verify(mapper,never()).updateTurnOrderSequence(anyLong(),anyLong(),anyInt());
    }
    @Test void existingFuturePayoutBlocksReassignmentWithoutCancellingRequest() {
        group.setStatus("02"); when(mapper.existsUpcomingTurnOrderPayout(20L,3)).thenReturn(true);
        assertThatThrownBy(() -> service.update(10L,30L,request(30,3,40,4))).isInstanceOf(ApiException.class);
        verify(mapper,never()).insertTurnOrderPlan(any());
    }
}
