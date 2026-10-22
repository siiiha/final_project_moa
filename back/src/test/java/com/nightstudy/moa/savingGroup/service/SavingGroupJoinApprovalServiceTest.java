package com.nightstudy.moa.savingGroup.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

// AI작성: Spring 트랜잭션 프록시를 통해 검증·UPDATE 순서와 실패 시 롤백 요청을 확인한다.
class SavingGroupJoinApprovalServiceTest {
    private SavingGroupMapper mapper;
    private PlatformTransactionManager manager;
    private SavingGroupService service;
    private SavingGroupJoinApprovalTargetRow request;
    private SimpleTransactionStatus transaction;

    @BeforeEach
    void setUp() {
        mapper = mock(SavingGroupMapper.class);
        manager = mock(PlatformTransactionManager.class);
        transaction = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(transaction);
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(new SavingGroupServiceImpl(mapper));
        proxy.addAdvice(interceptor);
        service = (SavingGroupService) proxy.getProxy();

        request = new SavingGroupJoinApprovalTargetRow();
        request.setCrewMemberId(50L);
        request.setMemberId(20L);
        request.setGroupId(30L);
        request.setMembershipStatus("PENDING");
        var target = new SavingGroupJoinTargetRow();
        target.setGroupId(30L);
        target.setOwnerMemberId(40L);
        target.setStatus("01");
        target.setMaxMembers(5);
        var owner = new SavingGroupMembershipRelationRow();
        owner.setMemberRole("OWNER");
        owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupJoinApprovalTarget(50L)).thenReturn(request);
        when(mapper.selectSavingGroupJoinMemberForUpdate(20L)).thenReturn(new SavingGroupJoinMemberRow());
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(target);
        when(mapper.selectSavingGroupJoinApprovalTargetForUpdate(50L)).thenReturn(request);
        when(mapper.selectSavingGroupMembershipForUpdate(30L, 40L)).thenReturn(owner);
        when(mapper.selectApprovedMemberCountForUpdate(30L)).thenReturn(4L);
        when(mapper.selectApprovedSavingGroupCount(20L)).thenReturn(4L);
        when(mapper.approveSavingGroupJoinRequest(50L, 30L, 40L)).thenReturn(1);
    }

    @Test
    void 하나의트랜잭션에서검증후기존행만승인한다() {
        service.approveSavingGroupJoin(10L, 50L, 40L);
        var order = inOrder(manager, mapper);
        order.verify(manager).getTransaction(argThat(def -> def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED));
        order.verify(mapper).selectSavingGroupJoinApprovalTarget(50L);
        order.verify(mapper).selectSavingGroupJoinMemberForUpdate(20L);
        order.verify(mapper).selectSavingGroupJoinTargetForUpdate(10L);
        order.verify(mapper).selectSavingGroupJoinApprovalTargetForUpdate(50L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(30L, 40L);
        order.verify(mapper).selectApprovedMemberCountForUpdate(30L);
        order.verify(mapper).selectApprovedSavingGroupCount(20L);
        order.verify(mapper).approveSavingGroupJoinRequest(50L, 30L, 40L);
        order.verify(manager).commit(transaction);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void 영향행수가하나가아니면롤백한다(int affected) {
        when(mapper.approveSavingGroupJoinRequest(50L, 30L, 40L)).thenReturn(affected);
        ApiException error = assertThrows(ApiException.class, () -> service.approveSavingGroupJoin(10L, 50L, 40L));
        assertEquals("JOIN_REQUEST_APPROVAL_FAILED", error.getCode());
        verify(manager).rollback(transaction);
        verify(manager, never()).commit(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "REJECTED", "LEFT", "EXPELLED"})
    void 중복승인과대기아닌신청은UPDATE하지않는다(String status) {
        request.setMembershipStatus(status);
        assertEquals("JOIN_REQUEST_NOT_PENDING", assertThrows(ApiException.class,
                () -> service.approveSavingGroupJoin(10L, 50L, 40L)).getCode());
        verify(mapper, never()).approveSavingGroupJoinRequest(any(), any(), any());
        verify(manager).rollback(transaction);
    }

    @Test
    void DB예외는롤백하고전파한다() {
        var failure = new org.springframework.dao.DataAccessResourceFailureException("테스트용 DB 실패");
        when(mapper.approveSavingGroupJoinRequest(50L, 30L, 40L)).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class, () -> service.approveSavingGroupJoin(10L, 50L, 40L)));
        verify(manager).rollback(transaction);
    }

    @Test
    void 다른계주는저장하지않는다() {
        assertEquals("SAVING_GROUP_OWNER_REQUIRED", assertThrows(ApiException.class,
                () -> service.approveSavingGroupJoin(10L, 50L, 41L)).getCode());
        verify(mapper, never()).approveSavingGroupJoinRequest(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"capacity", "joined"})
    void 제한도달시저장하지않는다(String limit) {
        if ("capacity".equals(limit)) {
            when(mapper.selectApprovedMemberCountForUpdate(30L)).thenReturn(5L);
        } else {
            when(mapper.selectApprovedSavingGroupCount(20L)).thenReturn(5L);
        }
        assertThrows(ApiException.class, () -> service.approveSavingGroupJoin(10L, 50L, 40L));
        verify(mapper, never()).approveSavingGroupJoinRequest(any(), any(), any());
    }

    // AI작성: 실제 매퍼 전체를 읽어 승인 SQL의 바인딩·저장값과 조건부 UPDATE를 검사한다.
    @Test
    void 실제매퍼의승인과재신청SQL은저장정책을지킨다() throws Exception {
        var config = new org.apache.ibatis.session.Configuration();
        config.getTypeAliasRegistry().registerAlias("SavingGroupCreationRequest",
                com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest.class);
        try (var stream = getClass().getClassLoader().getResourceAsStream("mappers/SavingGroupMapper.xml")) {
            new org.apache.ibatis.builder.xml.XMLMapperBuilder(stream, config, "mappers/SavingGroupMapper.xml", config.getSqlFragments()).parse();
        }
        String prefix = "com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper.";
        var mapped = config.getMappedStatement(prefix + "approveSavingGroupJoinRequest");
        assertEquals(org.apache.ibatis.mapping.SqlCommandType.UPDATE, mapped.getSqlCommandType());
        var bound = mapped.getBoundSql(java.util.Map.of("crewMemberId", 50L, "groupId", 30L, "approvedBy", 40L));
        String sql = bound.getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("membership_status = 'APPROVED'"));
        assertTrue(sql.contains("member_role = 'MEMBER'"));
        assertTrue(sql.contains("joined_at = CURRENT_TIMESTAMP"));
        assertTrue(sql.contains("approved_at = CURRENT_TIMESTAMP"));
        assertTrue(sql.contains("WHERE crew_member_id = ? AND group_id = ? AND membership_status = 'PENDING'"));
        assertEquals(java.util.List.of("approvedBy", "crewMemberId", "groupId"),
                bound.getParameterMappings().stream().map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList());
        String reapply = config.getMappedStatement(prefix + "reactivateLeftSavingGroupMembership")
                .getBoundSql(java.util.Map.of("memberId", 20L, "groupId", 30L)).getSql();
        assertTrue(reapply.contains("approved_by = NULL"));
        assertTrue(reapply.contains("approved_at = NULL"));
    }
}
