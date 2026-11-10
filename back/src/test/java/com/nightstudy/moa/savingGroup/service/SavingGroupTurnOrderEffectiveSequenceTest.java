package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

// AI작성: 예약 적용월과 운영 기간당 한 번 지급 정책의 공통 검증을 확인한다.
class SavingGroupTurnOrderEffectiveSequenceTest {
    @Test void completedMemberCannotReceiveAgainEvenWithoutPlan() {
        var mapper = mock(SavingGroupMapper.class);
        when(mapper.existsCompletedTurnOrderPayout(20L,30L)).thenReturn(true);
        assertThatThrownBy(() -> SavingGroupTurnOrderServiceImpl.resolveSequence(mapper,20L,30L,1,1)).isInstanceOf(ApiException.class);
    }
    @Test void resolvesPlanUsingRequestedRoundMonthAndPreservesLegacyBeforeEffectiveMonth() {
        var mapper = mock(SavingGroupMapper.class);
        when(mapper.selectTurnOrderSavingGroupId(20L)).thenReturn(10L);
        var group = new SavingGroupTurnOrderPlanRow(); group.setStartDate(LocalDate.of(2026,10,1));
        when(mapper.selectTurnOrderGroupForUpdate(10L)).thenReturn(group);
        assertThat(SavingGroupTurnOrderServiceImpl.resolveSequence(mapper,20L,30L,2,2)).isEqualTo(2);
        verify(mapper).selectTurnOrderPlan(10L,LocalDate.of(2026,11,1));
        var plan = new SavingGroupTurnOrderPlanRow(); plan.setTurnOrderPlanId(99L);
        when(mapper.selectTurnOrderPlan(10L,LocalDate.of(2026,12,1))).thenReturn(plan);
        var member = SavingGroupTurnOrderAssignmentServiceTest.member("30",false); member.setSequenceNumber(3);
        // AI작성: 이전 회차 미지급 계원의 고정 순번을 새 예약에 보존해도 다음 회차 지급이 가능하다.
        var previous = SavingGroupTurnOrderAssignmentServiceTest.member("50",false); previous.setSequenceNumber(2);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(List.of(member, previous));
        when(mapper.selectTurnOrderPlanMembers(99L)).thenReturn(List.of(previous, member));
        assertThat(SavingGroupTurnOrderServiceImpl.resolveSequence(mapper,20L,30L,1,3)).isEqualTo(3);
        when(mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(List.of(member,SavingGroupTurnOrderAssignmentServiceTest.member("40",false)));
        assertThatThrownBy(() -> SavingGroupTurnOrderServiceImpl.resolveSequence(mapper,20L,30L,1,3)).isInstanceOf(ApiException.class);
    }
}
