package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.*;

// AI작성: 롤백·커밋 통지에 따라 외부 결과를 구분하고 불확실 상태를 보존한다.
class VirtualAccountApprovalServiceTest {
    VirtualAccountIssueService issuer = mock(VirtualAccountIssueService.class);
    VirtualAccountStorageService storage = mock(VirtualAccountStorageService.class);
    VirtualAccountIssueStateService state = mock(VirtualAccountIssueStateService.class);
    VirtualAccountApprovalServiceImpl service = new VirtualAccountApprovalServiceImpl(issuer, storage, state);
    VirtualAccountIssueRequest request = new VirtualAccountIssueRequest(1L, 2L, "moa_test_order");
    @BeforeEach void setup() { TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void clear() { TransactionSynchronizationManager.clearSynchronization(); }
    void complete(int status) { TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status)); }
    @Test void savesBeforeApprovalAndRetainsUnknownOnLaterRollback() {
        when(state.begin(1L, 2L)).thenReturn(request);
        var result = new VirtualAccountIssueResult("00", "X000000000000", java.time.OffsetDateTime.now().plusDays(7));
        when(issuer.issue(request)).thenReturn(result); when(issuer.accountHolderName()).thenReturn("모아테스트");
        service.issueAndSave(1L, 2L);
        var order = inOrder(state, issuer, storage);
        order.verify(state).begin(1L, 2L); order.verify(issuer).issue(request);
        order.verify(storage).save(1L, 2L, result, "모아테스트");
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(state).recordFailure(1L, "UNKNOWN", "ACCOUNT_ISSUE_UNKNOWN");
    }
    @Test void committedApprovalDoesNotRecordFailure() {
        when(state.begin(1L, 2L)).thenReturn(request);
        service.issueAndSave(1L, 2L); complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(state, never()).recordFailure(any(), any(), any());
    }
    @Test void timeoutDoesNotSaveAccount() {
        when(state.begin(1L, 2L)).thenReturn(request);
        when(issuer.issue(request)).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "ACCOUNT_ISSUE_UNKNOWN", "확인 필요"));
        assertThatThrownBy(() -> service.issueAndSave(1L, 2L)).isInstanceOf(ApiException.class);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(storage, never()).save(any(), any(), any(), any());
        verify(state).recordFailure(1L, "UNKNOWN", "ACCOUNT_ISSUE_UNKNOWN");
    }
    @Test void rejectedRequestIsRecordedAsFailed() {
        when(state.begin(1L, 2L)).thenReturn(request);
        when(issuer.issue(request)).thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "ACCOUNT_ISSUE_REJECTED", "거절"));
        assertThatThrownBy(() -> service.issueAndSave(1L, 2L)).isInstanceOf(ApiException.class);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(state).recordFailure(1L, "FAILED", "ACCOUNT_ISSUE_REJECTED");
    }
    @Test void missingConfigurationDoesNotCreateHistory() {
        doThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TOSS_TEST_NOT_CONFIGURED", "설정 필요"))
                .when(issuer).requireConfigured();
        assertThatThrownBy(() -> service.issueAndSave(1L, 2L)).isInstanceOf(ApiException.class);
        verifyNoInteractions(state); verify(issuer, never()).issue(any());
    }
}
