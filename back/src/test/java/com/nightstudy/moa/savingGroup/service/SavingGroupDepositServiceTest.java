package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// AI작성: 실제 금융 객체 없이 인증·상태·최초 기준·확인 소비·저장 실패를 검증한다.
class SavingGroupDepositServiceTest {
    SavingGroupMapper mapper;
    SavingGroupDepositServiceImpl service;
    final LocalDateTime now = LocalDateTime.of(2026, 11, 6, 12, 0);
    final String key = "mock-request-key-0001";
    @BeforeEach void setup() {
        mapper = mock(SavingGroupMapper.class);
        service = new SavingGroupDepositServiceImpl(mapper, () -> {});
        var group = new SavingGroupPayoutTargetRow(); group.setStatus("02"); group.setGroupId(10L);
        when(mapper.selectPayoutTargetForUpdate(10L)).thenReturn(group);
        relation("APPROVED", "MEMBER");
        when(mapper.existsMember(20L)).thenReturn(true);
        when(mapper.selectDepositAccountForUpdate(10L)).thenReturn(30L);
        when(mapper.selectDepositNow()).thenReturn(now);
        when(mapper.insertDepositTransaction(any())).thenAnswer(inv -> {
            ((SavingGroupDepositRow) inv.getArgument(0)).setTransactionId(40L); return 1;
        });
        when(mapper.insertDepositRequest(any())).thenReturn(1);
        when(mapper.refreshDepositConfirmation(any())).thenReturn(1);
        when(mapper.completeDepositRequest(any())).thenReturn(1);
        when(mapper.selectSavingGroupBalance(10L)).thenReturn(new BigDecimal("11000"));
    }
    void relation(String status, String role) {
        var r = new SavingGroupMembershipRelationRow(); r.setMembershipStatus(status); r.setMemberRole(role);
        when(mapper.selectSavingGroupMembershipForUpdate(10L, 20L)).thenReturn(r);
    }
    SavingGroupDepositRequest request() { return new SavingGroupDepositRequest(new BigDecimal("1000"), key); }
    SavingGroupDepositRow pending() {
        var row = new SavingGroupDepositRow(); row.setDepositRequestId(50L); row.setAccountId(30L); row.setMemberId(20L);
        row.setAmount(new BigDecimal("1000")); row.setRequestKey(key); row.setRequestedAt(now);
        row.setStatus("PENDING_CONFIRMATION"); row.setWindowAnchorRequestId(1L); return row;
    }
    @ParameterizedTest @ValueSource(strings = {"OWNER", "MANAGER", "MEMBER"})
    void approvedRolesCreateInternalTransaction(String role) {
        relation("APPROVED", role);
        var result = service.deposit(10L, 20L, request());
        assertThat(result.status()).isEqualTo("COMPLETED"); assertThat(result.balance()).isEqualByComparingTo("11000");
        verify(mapper).insertDepositTransaction(argThat(r -> r.getMemberId().equals(20L)
                && r.getAmount().compareTo(new BigDecimal("1000")) == 0 && r.getCompletedAt().equals(now)));
        verify(mapper).insertDepositRequest(argThat(r -> "COMPLETED".equals(r.getStatus()) && r.getTransactionId().equals(40L)));
    }
    @ParameterizedTest @ValueSource(strings = {"LEFT", "PENDING", "EXPELLED", "REJECTED"})
    void rejectsNonCurrentMembers(String status) {
        relation(status, "MEMBER");
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @Test void rejectsAbsentAndOtherGroupMembers() {
        when(mapper.selectSavingGroupMembershipForUpdate(10L, 20L)).thenReturn(null);
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @Test void rejectsAnonymousDeletedMissingAndInactiveTargets() {
        assertThatThrownBy(() -> service.deposit(10L, null, request())).isInstanceOf(ApiException.class);
        when(mapper.existsMember(20L)).thenReturn(false);
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        when(mapper.existsMember(20L)).thenReturn(true);
        when(mapper.selectDepositAccountForUpdate(10L)).thenReturn(null);
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        var inactive = new SavingGroupPayoutTargetRow(); inactive.setStatus("01");
        when(mapper.selectPayoutTargetForUpdate(10L)).thenReturn(inactive);
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        when(mapper.selectPayoutTargetForUpdate(10L)).thenReturn(null);
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.01", "10000000000000"})
    void rejectsInvalidAmounts(String amount) {
        assertThatThrownBy(() -> service.deposit(10L, 20L, new SavingGroupDepositRequest(new BigDecimal(amount), key)))
                .isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @Test void duplicateNeedsConfirmationAndConfirmReplayDoesNotCreateAnotherTransaction() {
        var anchor = pending(); anchor.setDepositRequestId(1L);
        when(mapper.selectDepositAnchorForUpdate(30L, 20L, new BigDecimal("1000"), now.minusMinutes(5))).thenReturn(anchor);
        var response = service.deposit(10L, 20L, request());
        assertThat(response.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(response.confirmationExpiresAt()).isEqualTo(now.plusMinutes(5));
        var captor = org.mockito.ArgumentCaptor.forClass(SavingGroupDepositRow.class);
        verify(mapper).insertDepositRequest(captor.capture());
        var row = captor.getValue(); row.setDepositRequestId(50L);
        assertThat(row.getConfirmationTokenHash()).hasSize(64).isNotEqualTo(response.confirmationToken());
        verify(mapper, never()).insertDepositTransaction(any());
        when(mapper.selectDepositRequestForUpdate(20L, key)).thenReturn(row);
        var confirm = new SavingGroupDepositConfirmRequest(key, response.confirmationToken());
        assertThat(service.confirm(10L, 20L, confirm).status()).isEqualTo("COMPLETED");
        row.setStatus("COMPLETED"); row.setConfirmationTokenHash(null);
        service.confirm(10L, 20L, confirm);
        verify(mapper, times(1)).insertDepositTransaction(any());
        assertThat(row.getWindowAnchorRequestId()).isEqualTo(1L);
    }
    @Test void invalidExpiredAndCrossAccountConfirmCannotSave() {
        var row = pending(); row.setConfirmationTokenHash("0".repeat(64)); row.setConfirmationExpiresAt(now.plusMinutes(5));
        when(mapper.selectDepositRequestForUpdate(20L, key)).thenReturn(row);
        var confirm = new SavingGroupDepositConfirmRequest(key, "A".repeat(43));
        assertThatThrownBy(() -> service.confirm(10L, 20L, confirm)).isInstanceOf(ApiException.class);
        row.setConfirmationExpiresAt(now);
        assertThatThrownBy(() -> service.confirm(10L, 20L, confirm)).isInstanceOf(ApiException.class);
        row.setAccountId(99L);
        assertThatThrownBy(() -> service.confirm(10L, 20L, confirm)).isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @Test void retriesRefreshPendingTokenAndRejectChangedPayload() {
        var row = pending(); when(mapper.selectDepositRequestForUpdate(20L, key)).thenReturn(row);
        String first = service.deposit(10L, 20L, request()).confirmationToken();
        String second = service.deposit(10L, 20L, request()).confirmationToken();
        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> service.confirm(10L, 20L, new SavingGroupDepositConfirmRequest(key, first)))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.deposit(10L, 20L, new SavingGroupDepositRequest(BigDecimal.TEN, key)))
                .isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositTransaction(any());
    }
    @Test void failedTransactionInsertCannotCreateCompletedRequest() {
        doReturn(0).when(mapper).insertDepositTransaction(any());
        assertThatThrownBy(() -> service.deposit(10L, 20L, request())).isInstanceOf(ApiException.class);
        verify(mapper, never()).insertDepositRequest(any());
    }
}
