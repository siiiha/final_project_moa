package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: JDBC 연결 대역과 실제 Spring 트랜잭션 프록시로 전파·롤백 콜백을 검증한다.
// DB SQL·잠금·영속성은 이 테스트의 검증 범위가 아니며 별도 MySQL 테스트가 담당한다.
class SavingGroupAccountApprovalTransactionTest {
    final SavingGroupMapper groups = mock(SavingGroupMapper.class);
    final VirtualAccountStorageMapper accounts = mock(VirtualAccountStorageMapper.class);
    final VirtualAccountIssueService issuer = mock(VirtualAccountIssueService.class);
    final List<Connection> connections = new ArrayList<>();
    DataSourceTransactionManager manager;
    SavingGroupCreationService creation;
    final AtomicReference<VirtualAccountIssueRecord> issue = new AtomicReference<>();

    @BeforeEach void setup() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenAnswer(call -> {
            var connection = mock(Connection.class);
            when(connection.getAutoCommit()).thenReturn(true);
            connections.add(connection);
            return connection;
        });
        manager = new DataSourceTransactionManager(dataSource);
        var state = proxy(new VirtualAccountIssueStateServiceImpl(accounts), VirtualAccountIssueStateService.class);
        var storage = proxy(new VirtualAccountStorageServiceImpl(accounts, AccountNumberCipherTest.cipher()), VirtualAccountStorageService.class);
        var approval = proxy(new VirtualAccountApprovalServiceImpl(issuer, storage, state), VirtualAccountApprovalService.class);
        creation = proxy(new SavingGroupCreationServiceImpl(groups, mock(FileStorageService.class), approval), SavingGroupCreationService.class);
        var application = new SavingGroupCreationApplicationDetailResponse();
        application.setApprovalId(1L); application.setGroupId(2L); application.setType("CREATE"); application.setStatus("PENDING");
        when(groups.selectSavingGroupCreationApplicationForUpdate(1L)).thenReturn(application);
        when(accounts.selectIssue(1L)).thenAnswer(call -> issue.get());
        when(accounts.insertIssue(any())).thenAnswer(call -> { issue.set(call.getArgument(0)); return 1; });
        when(accounts.failIssue(eq(1L), anyString(), anyString())).thenReturn(1);
        when(issuer.issue(any())).thenReturn(new VirtualAccountIssueResult("00", "X000000000000", OffsetDateTime.now().plusDays(7)));
        when(issuer.accountHolderName()).thenReturn("모아테스트");
        when(accounts.insertAccount(any())).thenAnswer(call -> {
            call.<EncryptedGroupAccountRow>getArgument(0).setAccountId(3L); return 1;
        });
        when(accounts.completeIssue(1L, 3L)).thenReturn(1);
        when(groups.approveSavingGroupCreationApplication(1L)).thenReturn(1);
        when(groups.publishApprovedSavingGroup(2L)).thenReturn(1);
        when(groups.approveSavingGroupCreationOwnerMembership(2L, 4L)).thenReturn(1);
    }

    <T> T proxy(T target, Class<T> contract) {
        var factory = new ProxyFactory(target);
        factory.setInterfaces(contract);
        factory.addAdvice(new TransactionInterceptor(manager, new AnnotationTransactionAttributeSource()));
        return contract.cast(factory.getProxy());
    }

    @Test void commitsReservationSeparatelyAndAccountWithApproval() throws Exception {
        creation.approveCreationApplication(1L, 4L);
        assertThat(connections).hasSize(2);
        verify(connections.get(1)).commit();
        verify(connections.get(0)).commit();
        var order = inOrder(issuer, accounts, groups);
        order.verify(issuer).issue(any());
        order.verify(accounts).insertAccount(any());
        order.verify(groups).approveSavingGroupCreationApplication(1L);
        verify(accounts, never()).failIssue(any(), any(), any());
    }

    @Test void approvalFailureRollsBackBeforeSeparateFailureRecord() throws Exception {
        when(groups.publishApprovedSavingGroup(2L)).thenReturn(0);
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 4L)).isInstanceOf(ApiException.class);
        assertThat(connections).hasSize(3);
        var order = inOrder(connections.get(0), accounts, connections.get(2));
        order.verify(connections.get(0)).rollback();
        order.verify(accounts).failIssue(1L, "UNKNOWN", "ACCOUNT_ISSUE_UNKNOWN");
        order.verify(connections.get(2)).commit();
        verify(connections.get(1)).commit();
    }

    @Test void storageFailureDoesNotApproveAndRetryDoesNotIssueAgain() throws Exception {
        doReturn(0).when(accounts).insertAccount(any());
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 4L)).isInstanceOf(ApiException.class);
        verify(connections.get(0)).rollback();
        verify(groups, never()).approveSavingGroupCreationApplication(any());
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 4L)).isInstanceOf(ApiException.class);
        verify(issuer, times(1)).issue(any());
        verify(accounts).failIssue(1L, "UNKNOWN", "ACCOUNT_ISSUE_UNKNOWN");
    }

    @Test void failureRecordFailureKeepsOriginalExceptionAndBlocksRetry() {
        when(accounts.failIssue(eq(1L), anyString(), anyString())).thenThrow(new IllegalStateException("테스트 저장 실패"));
        when(issuer.issue(any())).thenThrow(new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                "ACCOUNT_ISSUE_UNKNOWN", "발급 확인 필요"));
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 4L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("ACCOUNT_ISSUE_UNKNOWN"));
        assertThat(issue.get()).isNotNull();
        assertThatThrownBy(() -> creation.approveCreationApplication(1L, 4L)).isInstanceOf(ApiException.class);
        verify(issuer, times(1)).issue(any());
    }
}
