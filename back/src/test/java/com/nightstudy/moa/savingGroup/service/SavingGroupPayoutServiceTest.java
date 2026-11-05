package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// AI작성: 실제 금융 호출 없이 정상 요청·권한·확정 금액·중복·롤백 증빙 정리를 검증한다.
class SavingGroupPayoutServiceTest {
    SavingGroupMapper mapper;
    SavingGroupReceiptStorage storage;
    SavingGroupPayoutServiceImpl service;
    SavingGroupPayoutTargetRow group;
    SavingGroupMembershipRelationRow member;
    SavingGroupPayoutRoundRow round;
    SavingGroupPayoutRequest request;
    MockMultipartFile file = new MockMultipartFile("receipt", "receipt.png", "image/png", new byte[]{1});
    @BeforeEach void setup() {
        mapper = mock(SavingGroupMapper.class); storage = mock(SavingGroupReceiptStorage.class);
        service = new SavingGroupPayoutServiceImpl(mapper, storage);
        group = new SavingGroupPayoutTargetRow(); group.setGroupId(1L); group.setGroupType("순번계"); group.setStatus("02");
        group.setStartDate(LocalDate.now(ZoneId.of("Asia/Seoul")).withDayOfMonth(1));
        when(mapper.selectPayoutTargetForUpdate(1L)).thenReturn(group);
        member = new SavingGroupMembershipRelationRow(); member.setMembershipStatus("APPROVED"); member.setMemberRole("MEMBER");
        when(mapper.selectSavingGroupMembershipForUpdate(1L, 2L)).thenReturn(member);
        round = new SavingGroupPayoutRoundRow(); round.setRoundId(3L); round.setRoundNumber(1);
        round.setBaseDuesAmount(new BigDecimal("100099")); round.setApprovedMemberCount(5); round.setPreviousPaidMemberCount(2);
        round.setAdditionalDuesAmount(new BigDecimal("1000")); round.setPayoutAmount(new BigDecimal("502495"));
        when(mapper.selectPayoutRoundForUpdate(1L, 1)).thenReturn(round); when(mapper.selectPayoutSequence(1L, 2L)).thenReturn(1);
        request = new SavingGroupPayoutRequest();
        when(mapper.insertRoundPayout(any())).thenAnswer(inv -> { ((SavingGroupPayoutInsertRow) inv.getArgument(0)).setId(8L); return 1; });
        when(mapper.insertReceiptExpense(any())).thenAnswer(inv -> { ((SavingGroupPayoutInsertRow) inv.getArgument(0)).setId(9L); return 1; });
        when(storage.prepare(file)).thenReturn(new SavingGroupReceiptStorage.Prepared(new byte[]{1}, "a".repeat(64), "png"));
        when(storage.store(any())).thenReturn("receipt.png");
    }
    void expense(String role) { group.setGroupType("친목계"); member.setMemberRole(role); request.setAmount(new BigDecimal("1000")); request.setTitle("모임 식비"); }
    void fails(String code) {
        assertThat(catchThrowableOfType(() -> service.createPayout(1L, 2L, request, "친목계".equals(group.getGroupType()) ? file : null), ApiException.class).getCode()).isEqualTo(code);
        verify(mapper, never()).insertRoundPayout(any()); verify(mapper, never()).insertReceiptExpense(any());
    }
    @Test void rotationUsesSnapshotAndNoReceiptOrTransfer() {
        assertThat(service.createPayout(1L, 2L, request, null).amount()).isEqualByComparingTo("502495");
        var order = inOrder(mapper); order.verify(mapper).selectPayoutTargetForUpdate(1L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(1L, 2L);
        verifyNoInteractions(storage);
    }
    @Test void wrongTurn() { when(mapper.selectPayoutSequence(1L, 2L)).thenReturn(2); fails("PAYOUT_NOT_YOUR_TURN"); }
    @Test void missingRound() { when(mapper.selectPayoutRoundForUpdate(1L, 1)).thenReturn(null); fails("PAYOUT_ROUND_MISSING"); }
    @Test void missingSnapshot() { round.setBaseDuesAmount(null); fails("PAYOUT_SNAPSHOT_MISSING"); }
    @Test void invalidSnapshot() { round.setAdditionalDuesAmount(new BigDecimal("1001")); fails("PAYOUT_SNAPSHOT_MISSING"); }
    @Test void alreadyRequestedApprovedOrCompleted() { when(mapper.existsBlockingRoundPayout(3L)).thenReturn(true); fails("PAYOUT_DUPLICATE"); }
    @Test void missingGroup() { when(mapper.selectPayoutTargetForUpdate(1L)).thenReturn(null); fails("NOT_FOUND"); }
    @Test void unauthenticated() { assertThat(catchThrowableOfType(() -> service.createPayout(1L, null, request, null), ApiException.class).getCode()).isEqualTo("UNAUTHORIZED"); verifyNoInteractions(storage); }
    @Test void nonmember() { when(mapper.selectSavingGroupMembershipForUpdate(1L, 2L)).thenReturn(null); fails("PAYOUT_FORBIDDEN"); }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "LEFT", "EXPELLED", "REJECTED"})
    void inactiveMembership(String status) { member.setMembershipStatus(status); fails("PAYOUT_FORBIDDEN"); }
    @ParameterizedTest @ValueSource(strings = {"00", "01", "03", "04"})
    void inactiveGroup(String status) { group.setStatus(status); fails("PAYOUT_INACTIVE"); }
    @Test void unsupportedType() { group.setGroupType("기타"); fails("PAYOUT_INVALID_TYPE"); }
    @Test void notStarted() { group.setStartDate(LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1)); fails("PAYOUT_NOT_STARTED"); }
    @ParameterizedTest @ValueSource(strings = {"OWNER", "MANAGER"})
    void expenseOperators(String role) { expense(role); assertThat(service.createPayout(1L, 2L, request, file).status()).isEqualTo("PENDING"); verify(mapper, never()).insertRoundPayout(any()); }
    @Test void expenseMemberDenied() { expense("MEMBER"); fails("PAYOUT_FORBIDDEN"); }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.1", "10000000000000"})
    void invalidAmount(String value) { expense("OWNER"); request.setAmount(new BigDecimal(value)); fails("PAYOUT_INVALID_INPUT"); }
    @Test void duplicateReceipt() { expense("OWNER"); when(mapper.existsBlockingReceiptExpense(1L, "a".repeat(64))).thenReturn(true); fails("PAYOUT_DUPLICATE"); verify(storage, never()).store(any()); }
    @Test void rejectedReceiptCanBeRequestedAgain() { expense("MANAGER"); when(mapper.existsBlockingReceiptExpense(1L, "a".repeat(64))).thenReturn(false); assertThat(service.createPayout(1L, 2L, request, file).requestId()).isEqualTo(9L); }
    @Test void transactionRollbackRemovesNewReceipt() {
        expense("OWNER"); TransactionSynchronizationManager.initSynchronization();
        try {
            service.createPayout(1L, 2L, request, file);
            for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(storage).delete("receipt.png");
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void managerCannotReadReceipt() {
        expense("MANAGER"); assertThat(catchThrowableOfType(() -> service.getReceipt(1L, 9L, 2L), ApiException.class).getCode()).isEqualTo("RECEIPT_FORBIDDEN");
        verify(storage, never()).read(any());
    }
    @Test void ownerReadsOnlyScopedReceipt() { expense("OWNER"); when(mapper.selectPayoutReceiptForOwner(1L, 9L)).thenReturn("receipt.png"); service.getReceipt(1L, 9L, 2L); verify(storage).read("receipt.png"); }
}
