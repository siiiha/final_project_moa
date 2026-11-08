package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI작성: 모임별 승인 관계와 장부 필수값을 내부 테스트 데이터로 검증한다.
class SavingGroupTransactionServiceTest {
    private final SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    private final VirtualAccountStorageMapper accounts = mock(VirtualAccountStorageMapper.class);
    private final SavingGroupServiceImpl service = new SavingGroupServiceImpl(
            mapper, accounts, mock(AccountNumberCipher.class), mock(NotificationService.class));

    private void membership(String role, String status) {
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        var relation = new SavingGroupMembershipRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(2L, 1L)).thenReturn(relation);
    }

    private void account() {
        var row = new EncryptedGroupAccountRow();
        row.setAccountId(3L);
        when(accounts.selectAccount(2L)).thenReturn(row);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MANAGER", "MEMBER"})
    void readsSecondPageAndAllowsZeroBalance(String role) {
        membership(role, "APPROVED");
        account();
        when(mapper.selectSavingGroupTransactionCount(3L)).thenReturn(11);
        var now = LocalDateTime.of(2026, 11, 8, 12, 0);
        when(mapper.selectSavingGroupTransactions(3L, 10L, 10))
                .thenReturn(List.of(new SavingGroupTransactionRow(now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO, "테스트")));
        var result = service.getSavingGroupTransactions(2L, 1L, 2);
        assertThat(result.items()).containsExactly(new SavingGroupTransactionResponse(now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO));
        assertThat(result.pageMeta().getTotalPages()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "LEFT", "EXPELLED", "REJECTED"})
    void deniesInactiveMembership(String status) {
        membership("MEMBER", status);
        assertThatThrownBy(() -> service.getSavingGroupTransactions(2L, 1L, 1)).isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void handlesMissingGroupMembershipAccountAndEmptyLedger() {
        assertThatThrownBy(() -> service.getSavingGroupTransactions(2L, 1L, 1)).isInstanceOf(ApiException.class);
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        assertThatThrownBy(() -> service.getSavingGroupTransactions(2L, 9L, 1)).isInstanceOf(ApiException.class);
        membership("MEMBER", "APPROVED");
        assertThatThrownBy(() -> service.getSavingGroupTransactions(2L, 1L, 1)).isInstanceOf(ApiException.class);
        account();
        assertThat(service.getSavingGroupTransactions(2L, 1L, 1).items()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsMissingMemo(String memo) {
        assertInvalidRow(memo, BigDecimal.ZERO);
    }

    @Test
    void rejectsMissingBalance() {
        assertInvalidRow("테스트", null);
    }

    private void assertInvalidRow(String memo, BigDecimal balance) {
        membership("MEMBER", "APPROVED");
        account();
        when(mapper.selectSavingGroupTransactionCount(3L)).thenReturn(1);
        when(mapper.selectSavingGroupTransactions(3L, 0L, 10))
                .thenReturn(List.of(new SavingGroupTransactionRow(LocalDateTime.now(), "DEPOSIT", BigDecimal.TEN, balance, memo)));
        assertThatThrownBy(() -> service.getSavingGroupTransactions(2L, 1L, 1))
                .isInstanceOf(ApiException.class).hasMessage("거래내역을 확인할 수 없습니다.");
    }
}
