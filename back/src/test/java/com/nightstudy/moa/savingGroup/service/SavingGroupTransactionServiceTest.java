package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.cardExpense.*;
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
    private final CardExpenseClassifierClient classifier = mock(CardExpenseClassifierClient.class);
    private final SavingGroupServiceImpl service = new SavingGroupServiceImpl(
            mapper, accounts, mock(AccountNumberCipher.class), mock(NotificationService.class), classifier);

    // AI작성: 분류 대상과 장애 시 거래 응답을 함께 검증한다.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void classifiesOnlyDemoExpensesAndKeepsLedgerOnFailure(boolean available) {
        membership("MEMBER", "APPROVED");
        account();
        var now = LocalDateTime.of(2026, 11, 8, 12, 0);
        when(mapper.selectSavingGroupTransactionCount(3L)).thenReturn(4);
        when(mapper.selectSavingGroupTransactions(3L, 0L, 10)).thenReturn(List.of(
                new SavingGroupTransactionRow(now, "DEPOSIT", BigDecimal.TEN, BigDecimal.TEN, "회비", "DUES", "MOCK"),
                new SavingGroupTransactionRow(now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO, "계금 지급", "PAYOUT", "MOCK"),
                new SavingGroupTransactionRow(now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO, "외부 거래"),
                new SavingGroupTransactionRow(123L, now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO, "동네밥상", "EXPENSE", "MOCK", null)));
        if (available) {
            when(classifier.classify("동네밥상", BigDecimal.TEN)).thenReturn(
                    new CardExpenseClassifierClient.ClassificationResult(CardExpenseCategory.FOOD, 0.9,
                            CardExpenseClassificationStatus.REVIEW_PENDING, CardExpenseClassificationSource.MACHINE_LEARNING, null));
        } else {
            when(classifier.classify("동네밥상", BigDecimal.TEN)).thenThrow(new ApiException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "CARD_EXPENSE_CLASSIFIER_UNAVAILABLE", "분류 실패"));
        }

        var items = service.getSavingGroupTransactions(2L, 1L, 1).items();
        assertThat(items).hasSize(4);
        assertThat(items.get(3)).isEqualTo(new SavingGroupTransactionResponse(123L, now, "WITHDRAWAL",
                BigDecimal.TEN, BigDecimal.ZERO, "동네밥상", "EXPENSE", available ? "FOOD" : null,
                available ? "REVIEW_PENDING" : null, false));
        verify(classifier).classify("동네밥상", BigDecimal.TEN);
        verifyNoMoreInteractions(classifier);
    }

    private void membership(String role, String status) {
        var target = new SavingGroupRow();
        target.setGroupId(2L);
        when(mapper.selectSavingGroupDetailBySavingGroupId(12L)).thenReturn(target);
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
        var result = service.getSavingGroupTransactions(12L, 1L, 2);
        assertThat(result.items()).containsExactly(new SavingGroupTransactionResponse(now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO));
        assertThat(result.pageMeta().getTotalPages()).isEqualTo(2);
    }

    // AI작성: 확인 필요 출금은 사용자가 저장한 최종 분류가 있어도 다시 수정할 수 있다.
    @Test
    void keepsReviewRequiredMockWithdrawalEditableAfterCategoryIsSaved() {
        membership("OWNER", "APPROVED");
        account();
        var now = LocalDateTime.of(2026, 11, 8, 12, 0);
        when(mapper.selectSavingGroupTransactionCount(3L)).thenReturn(1);
        when(mapper.selectSavingGroupTransactions(3L, 0L, 10)).thenReturn(List.of(
                new SavingGroupTransactionRow(123L, now, "WITHDRAWAL", BigDecimal.TEN, BigDecimal.ZERO,
                        "동네밥상", "EXPENSE", "MOCK", "FOOD")));
        when(classifier.classify("동네밥상", BigDecimal.TEN)).thenReturn(
                new CardExpenseClassifierClient.ClassificationResult(CardExpenseCategory.NEEDS_REVIEW, 0.5,
                        CardExpenseClassificationStatus.REVIEW_REQUIRED, CardExpenseClassificationSource.MACHINE_LEARNING, null));

        var item = service.getSavingGroupTransactions(12L, 1L, 1).items().getFirst();
        assertThat(item.category()).isEqualTo("FOOD");
        assertThat(item.classificationStatus()).isEqualTo("CONFIRMED");
        assertThat(item.categoryEditable()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "LEFT", "EXPELLED", "REJECTED"})
    void deniesInactiveMembership(String status) {
        membership("MEMBER", status);
        assertThatThrownBy(() -> service.getSavingGroupTransactions(12L, 1L, 1)).isInstanceOf(ApiException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void handlesMissingGroupMembershipAccountAndEmptyLedger() {
        assertThatThrownBy(() -> service.getSavingGroupTransactions(12L, 1L, 1)).isInstanceOf(ApiException.class);
        var target = new SavingGroupRow();
        target.setGroupId(2L);
        when(mapper.selectSavingGroupDetailBySavingGroupId(12L)).thenReturn(target);
        assertThatThrownBy(() -> service.getSavingGroupTransactions(12L, 9L, 1)).isInstanceOf(ApiException.class);
        membership("MEMBER", "APPROVED");
        assertThatThrownBy(() -> service.getSavingGroupTransactions(12L, 1L, 1)).isInstanceOf(ApiException.class);
        account();
        assertThat(service.getSavingGroupTransactions(12L, 1L, 1).items()).isEmpty();
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
        assertThatThrownBy(() -> service.getSavingGroupTransactions(12L, 1L, 1))
                .isInstanceOf(ApiException.class).hasMessage("거래내역을 확인할 수 없습니다.");
    }
}
