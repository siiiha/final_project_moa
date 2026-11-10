package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.SavingGroupTurnOrderPlanRow;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// AI작성: 실제 Spring 트랜잭션 프록시에서 두 번째 저장 실패가 커밋되지 않고 롤백되는지 확인한다.
// JDBC 영속 데이터와 MySQL 잠금 자체의 검증은 별도 전용 DB 테스트에서 수행한다.
class SavingGroupTurnOrderAssignmentTransactionTest {
    @ParameterizedTest @ValueSource(booleans={false,true})
    void partialFailureRollsBackImmediateAndScheduledAssignments(boolean scheduled) throws Exception {
        var fixture = new SavingGroupTurnOrderAssignmentServiceTest(); fixture.setup();
        if (scheduled) {
            fixture.group.setStatus("02");
            // AI작성: 현재 2회차 계원을 예약에 보존한 뒤 미래 순번 저장 실패를 재현한다.
            var current = SavingGroupTurnOrderAssignmentServiceTest.member("50", false);
            current.setSequenceNumber(2);
            when(fixture.mapper.selectTurnOrderAssignmentMembers(20L)).thenReturn(java.util.List.of(
                    SavingGroupTurnOrderAssignmentServiceTest.member("30", false),
                    SavingGroupTurnOrderAssignmentServiceTest.member("40", false), current));
            when(fixture.mapper.insertTurnOrderPlan(any())).thenAnswer(call -> {
                ((SavingGroupTurnOrderPlanRow)call.getArgument(0)).setTurnOrderPlanId(99L); return 1;
            });
            when(fixture.mapper.insertTurnOrderPlanMember(99L,50L,2)).thenReturn(1);
            when(fixture.mapper.insertTurnOrderPlanMember(99L,30L,3)).thenAnswer(call -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue(); return 1;
            });
            when(fixture.mapper.insertTurnOrderPlanMember(99L,40L,4)).thenReturn(0);
        } else {
            when(fixture.mapper.updateTurnOrderSequence(20L,30L,1)).thenAnswer(call -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue(); return 1;
            });
            when(fixture.mapper.updateTurnOrderSequence(20L,40L,2)).thenReturn(0);
        }
        var connection = mock(Connection.class); when(connection.getAutoCommit()).thenReturn(true);
        var source = mock(DataSource.class); when(source.getConnection()).thenReturn(connection);
        var factory = new ProxyFactory(fixture.service);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source),new AnnotationTransactionAttributeSource()));
        var service = (SavingGroupTurnOrderService)factory.getProxy();
        assertThatThrownBy(() -> service.update(10L,30L,SavingGroupTurnOrderAssignmentServiceTest.request(30,scheduled?3:1,40,scheduled?4:2)))
            .isInstanceOf(ApiException.class);
        verify(connection).rollback(); verify(connection,never()).commit();
        if (scheduled) verify(fixture.mapper).insertTurnOrderPlanMember(99L,40L,4);
        else verify(fixture.mapper).updateTurnOrderSequence(20L,40L,2);
    }
}
