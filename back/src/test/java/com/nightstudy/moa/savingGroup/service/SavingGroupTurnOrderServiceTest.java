package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 고정 랜덤 결과를 가정하지 않고 전체 회원·순번·예상액과 읽기 전용 호출 계약을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupTurnOrderServiceTest {
    @Mock SavingGroupMapper mapper;
    @InjectMocks SavingGroupServiceImpl service;
    SavingGroupTurnOrderContextRow group;
    void group(String type, String status) {
        group = new SavingGroupTurnOrderContextRow();
        group.setGroupId(20L); group.setGroupType(type); group.setStatus(status); group.setDuesAmount(new BigDecimal("1099"));
        when(mapper.selectTurnOrderRecommendationContext(10L)).thenReturn(group);
    }
    void relation(String role, String status) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(row);
    }
    void owner() { relation("OWNER", "APPROVED"); when(mapper.existsMember(30L)).thenReturn(true); }
    SavingGroupTurnOrderMemberRow member(String id) {
        var row = new SavingGroupTurnOrderMemberRow(); row.setMemberId(id); row.setNickname("테스트" + id); return row;
    }
    void fails(String code, Long id, Long user) {
        assertThatThrownBy(() -> service.getTurnOrderRecommendations(id, user))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
    @Test void allMembersAppearExactlyOnceAndNothingIsWritten() {
        group("순번계", "01"); owner();
        var rows = List.of(member("30"), member("9223372036854775807"), member("50"));
        when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, null)).thenReturn(rows);
        for (int attempt = 0; attempt < 10; attempt++) {
            var result = service.getTurnOrderRecommendations(10L, 30L);
            assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getMemberId)
                    .containsExactlyInAnyOrder("30", "9223372036854775807", "50");
            assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getRecommendedTurnOrder).containsExactly(1, 2, 3);
            assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getExpectedPayoutAmount)
                    .containsExactly(new BigDecimal("3297"), new BigDecimal("3307"), new BigDecimal("3317"));
        }
        assertThat(rows).extracting(SavingGroupTurnOrderMemberRow::getMemberId).containsExactly("30", "9223372036854775807", "50");
        verify(mapper, times(10)).selectTurnOrderRecommendationContext(10L);
        verify(mapper, times(10)).selectSavingGroupMembership(20L, 30L);
        verify(mapper, times(10)).existsMember(30L);
        verify(mapper, times(10)).selectTurnOrderRecommendationMembers(20L, 10L, null);
        verifyNoMoreInteractions(mapper);
    }
    // AI작성: 현재 회차 지급 전후 모두 제외하고 전체 승인 인원과 실제 회차로 예상액을 계산한다.
    @ParameterizedTest @ValueSource(booleans = {false, true})
    void recommendsOnlyFutureRounds(boolean currentPaid) {
        var month = runningGroup(2);
        when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, month)).thenReturn(List.of(
                member("30", 4, false), member("40", 1, true), member("50", 2, currentPaid),
                member("60", 3, false), member("70", 5, false)));
        var result = service.getTurnOrderRecommendations(10L, 30L);
        assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getMemberId)
                .containsExactlyInAnyOrder("30", "60", "70");
        assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getRecommendedTurnOrder)
                .containsExactly(3, 4, 5);
        assertThat(result).extracting(SavingGroupTurnOrderRecommendationResponse::getExpectedPayoutAmount)
                .containsExactly(new BigDecimal("5515"), new BigDecimal("5525"), new BigDecimal("5535"));
    }

    @Test void oneRemainingMemberHasNoRecommendation() {
        var month = runningGroup(3);
        when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, month)).thenReturn(List.of(
                member("30", 1, true), member("40", 2, true), member("50", 3, false), member("60", 4, false)));
        assertThat(service.getTurnOrderRecommendations(10L, 30L)).isEmpty();
    }

    @Test void rejectsAmbiguousCurrentRecipient() {
        var month = runningGroup(2);
        when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, month)).thenReturn(List.of(
                member("30", 2, false), member("40", 2, false)));
        fails("SAVING_GROUP_TURN_ORDER_CONFLICT", 10L, 30L);
    }

    private LocalDate runningGroup(int round) {
        group("순번계", "02"); owner();
        var month = LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfMonth(1);
        group.setStartDate(month.minusMonths(round - 1L));
        return month;
    }

    private SavingGroupTurnOrderMemberRow member(String id, int sequence, boolean paid) {
        var row = member(id);
        row.setSequenceNumber(sequence);
        row.setPaid(paid);
        return row;
    }
    @Test void unauthenticatedAndInvalidIds() {
        fails("UNAUTHORIZED", 10L, null); fails("UNAUTHORIZED", 10L, 0L);
        fails("BAD_REQUEST", null, 30L); fails("BAD_REQUEST", 0L, 30L); verifyNoInteractions(mapper);
    }
    @Test void missingGroup() { fails("SAVING_GROUP_NOT_FOUND", 10L, 30L); }
    @Test void nonMember() { group("순번계", "01"); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L); }
    @ParameterizedTest @ValueSource(strings = {"MANAGER", "MEMBER"})
    void wrongRole(String role) { group("순번계", "01"); relation(role, "APPROVED"); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L); }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "LEFT", "EXPELLED", "REJECTED"})
    void inactiveMembership(String status) { group("순번계", "01"); relation("OWNER", status); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L); }
    @Test void deletedOwner() { group("순번계", "01"); relation("OWNER", "APPROVED"); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 30L); }
    @Test void wrongType() { group("친목계", "01"); owner(); fails("SAVING_GROUP_TYPE_INVALID", 10L, 30L); }
    @ParameterizedTest @ValueSource(strings = {"03", "04", "", "UNKNOWN"})
    void wrongStatus(String status) { group("순번계", status); owner(); fails("SAVING_GROUP_ACTIVITY_RESTRICTED", 10L, 30L); }
    @Test void emptyResult() {
        group("순번계", "01"); owner(); when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, null)).thenReturn(List.of());
        assertThat(service.getTurnOrderRecommendations(10L, 30L)).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "0.5", "99999999999999"})
    void invalidDues(String dues) {
        group("순번계", "01"); owner(); group.setDuesAmount(new BigDecimal(dues));
        when(mapper.selectTurnOrderRecommendationMembers(20L, 10L, null)).thenReturn(List.of(member("30")));
        fails("SAVING_GROUP_DUES_INVALID", 10L, 30L);
    }
}
