package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

// AI작성: 실제 Spring 프록시가 읽기 전용·반복 읽기를 JDBC에 적용하는지 검증한다. 실제 DB 검증은 아니다.
class SavingGroupTurnOrderTransactionTest {
    SavingGroupMapper mapper;
    Connection connection;
    SavingGroupService service;
    @BeforeEach void setup() throws Exception {
        mapper = mock(SavingGroupMapper.class); connection = mock(Connection.class);
        var source = mock(DataSource.class); when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        var factory = new ProxyFactory(new SavingGroupServiceImpl(mapper, null, null));
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        service = (SavingGroupService) factory.getProxy();
    }
    @Test void readOnlyRepeatableReadCommitsWithoutWrites() throws Exception {
        var group = new SavingGroupTurnOrderContextRow(); group.setGroupId(20L); group.setGroupType("순번계"); group.setStatus("01");
        when(mapper.selectTurnOrderRecommendationContext(10L)).thenReturn(group);
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(owner); when(mapper.existsMember(30L)).thenReturn(true);
        when(mapper.selectTurnOrderRecommendationMembers(20L)).thenReturn(List.of());
        assertThat(service.getTurnOrderRecommendations(10L, 30L)).isEmpty();
        verify(connection).setReadOnly(true); verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        verify(connection).commit(); verify(connection, never()).rollback();
    }
    @Test void failureRollsBackReadOnlyTransaction() throws Exception {
        assertThatThrownBy(() -> service.getTurnOrderRecommendations(10L, 30L)).isInstanceOf(ApiException.class);
        verify(connection).setReadOnly(true); verify(connection).rollback(); verify(connection, never()).commit();
    }
}
