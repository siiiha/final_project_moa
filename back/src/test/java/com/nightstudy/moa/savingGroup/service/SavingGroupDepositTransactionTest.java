package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 실제 Spring 트랜잭션 프록시로 원장 뒤 보조 기록 저장 실패 시 롤백을 검증한다.
// SQL 영속성·잠금은 별도 명시적 MySQL 테스트가 검증한다.
class SavingGroupDepositTransactionTest {
    @Test void rollsBackTransactionWhenRequestSaveFails() throws Exception {
        var mapper = mock(SavingGroupMapper.class);
        var group = new SavingGroupPayoutTargetRow(); group.setStatus("02");
        var relation = new SavingGroupMembershipRelationRow(); relation.setMembershipStatus("APPROVED"); relation.setMemberRole("MEMBER");
        when(mapper.selectPayoutTargetForUpdate(10L)).thenReturn(group);
        when(mapper.selectSavingGroupMembershipForUpdate(10L, 20L)).thenReturn(relation);
        when(mapper.existsMember(20L)).thenReturn(true);
        when(mapper.selectDepositAccountForUpdate(10L)).thenReturn(30L);
        when(mapper.selectDepositNow()).thenReturn(LocalDateTime.of(2026, 11, 6, 12, 0));
        when(mapper.insertDepositTransaction(any())).thenAnswer(inv -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            ((SavingGroupDepositRow) inv.getArgument(0)).setTransactionId(40L); return 1;
        });
        when(mapper.insertDepositRequest(any())).thenReturn(0);
        var connection = mock(Connection.class); when(connection.getAutoCommit()).thenReturn(true);
        var source = mock(DataSource.class); when(source.getConnection()).thenReturn(connection);
        // AI작성: 트랜잭션 테스트도 실제 서비스와 같은 알림 의존성을 명시한다.
        NotificationService notificationService = mock(NotificationService.class);
        ProxyFactory proxy = new ProxyFactory(new SavingGroupDepositServiceImpl(mapper, notificationService, () -> {}));
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        var service = (SavingGroupDepositService) proxy.getProxy();
        assertThatThrownBy(() -> service.deposit(10L, 20L, new SavingGroupDepositRequest(BigDecimal.TEN, "mock-request-key-0001")))
                .isInstanceOf(com.nightstudy.moa.common.exception.ApiException.class);
        verify(mapper).insertDepositTransaction(any()); verify(connection).rollback(); verify(connection, never()).commit();
    }
}
