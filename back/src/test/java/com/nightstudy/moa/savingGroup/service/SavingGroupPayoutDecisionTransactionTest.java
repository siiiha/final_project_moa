package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.notification.service.NotificationService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import javax.sql.DataSource;
import java.sql.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 실제 Spring 트랜잭션 프록시와 JDBC 트랜잭션 매니저의 commit/rollback을 검증한다. DB 저장·잠금 검증은 아니다.
class SavingGroupPayoutDecisionTransactionTest {
    SavingGroupMapper mapper;
    Connection connection;
    SavingGroupService service;
    @BeforeEach void setup() throws Exception {
        mapper = mock(SavingGroupMapper.class); connection = mock(Connection.class);
        var datasource = mock(DataSource.class); when(datasource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        var factory = new ProxyFactory(new SavingGroupServiceImpl(mapper, null, null, org.mockito.Mockito.mock(NotificationService.class), org.mockito.Mockito.mock(com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassifierClient.class)));
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(datasource), new AnnotationTransactionAttributeSource()));
        service = (SavingGroupService) factory.getProxy();
        var group = new SavingGroupPayoutDecisionRow(); group.setGroupId(20L); group.setOwnerMemberId(30L);
        group.setStatus("02"); group.setGroupType("친목계");
        when(mapper.selectPayoutDecisionGroupForUpdate(10L)).thenReturn(group);
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(owner);
        var request = new SavingGroupPayoutDecisionRow(); request.setRequesterId(30L); request.setStatus("PENDING"); request.setHasEvidence(true);
        when(mapper.selectPayoutDecisionRequestForUpdate(20L, 40L, "친목계")).thenReturn(request);
        when(mapper.selectSavingGroupJoinMemberForUpdate(30L)).thenReturn(new SavingGroupJoinMemberRow());
    }
    @Test void approveCommits() throws Exception {
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "APPROVED", 30L, null)).thenReturn(1);
        service.approveSavingGroupPayoutRequest(10L, 40L, 30L);
        verify(connection).setAutoCommit(false); verify(connection).commit(); verify(connection, never()).rollback();
    }
    @Test void rejectCommits() throws Exception {
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "REJECTED", 30L, "확인 필요")).thenReturn(1);
        service.rejectSavingGroupPayoutRequest(10L, 40L, 30L, " 확인 필요 "); verify(connection).commit();
    }
    @Test void concurrentStateChangeRollsBack() throws Exception {
        assertThatThrownBy(() -> service.approveSavingGroupPayoutRequest(10L, 40L, 30L)).isInstanceOf(ApiException.class);
        verify(connection).rollback(); verify(connection, never()).commit();
    }
    @Test void storageFailureRollsBack() throws Exception {
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "APPROVED", 30L, null)).thenThrow(new IllegalStateException("테스트 저장 실패"));
        assertThatThrownBy(() -> service.approveSavingGroupPayoutRequest(10L, 40L, 30L)).isInstanceOf(IllegalStateException.class);
        verify(connection).rollback(); verify(connection, never()).commit();
    }
}
