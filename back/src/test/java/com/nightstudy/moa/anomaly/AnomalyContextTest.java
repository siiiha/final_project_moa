package com.nightstudy.moa.anomaly;

import com.nightstudy.moa.anomaly.dto.*;
import com.nightstudy.moa.anomaly.dto.TransactionBusinessContext.State;
import com.nightstudy.moa.anomaly.mapper.AnomalyContextMapper;
import com.nightstudy.moa.anomaly.service.AnomalyContextServiceImpl;
import com.nightstudy.moa.common.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI작성: 모의 Mapper 기반 검증이며 실제 지급 승인·DB FK 검증이 아니다.
class AnomalyContextTest {
    BusinessContextTransaction transaction() {
        var t=new BusinessContextTransaction();t.setTransactionId(2L);t.setGroupId(12L);
        t.setTransactionType("WITHDRAWAL");t.setAmount(new BigDecimal("50000.00"));return t;
    }
    PayoutContextRecord payout() {
        var p=new PayoutContextRecord();p.setPayoutRequestId(5L);p.setGroupId(12L);
        p.setAmount(new BigDecimal("50000"));p.setStatus("COMPLETED");p.setReviewerId(8L);
        p.setRequestedAt(LocalDateTime.of(2026,10,1,10,0));
        p.setReviewedAt(LocalDateTime.of(2026,10,2,10,0));
        p.setCompletedAt(LocalDateTime.of(2026,10,3,10,0));return p;
    }
    AnomalyContextMapper mapper() {
        var m=mock(AnomalyContextMapper.class);when(m.findTransaction(12L,2L)).thenReturn(transaction());
        when(m.findPayoutLinks(2L)).thenReturn(List.of(payout()));return m;
    }
    @Test void explicitCompletedLinkReturnsContextNotNormalVerdict() {
        var m=mapper();var result=new AnomalyContextServiceImpl(m).findContext(12L,2L);
        assertEquals(State.LINKED_COMPLETED_PAYOUT,result.state());assertEquals(5L,result.payoutRequestId());
        assertTrue(result.message().contains("모델 경고는 별도로"));
    }
    @Test void noLinkIsUnknownNotNormalOrAnomalous() {
        var m=mapper();when(m.findPayoutLinks(2L)).thenReturn(List.of());
        var result=new AnomalyContextServiceImpl(m).findContext(12L,2L);
        assertEquals(State.NO_LINK,result.state());assertNull(result.payoutRequestId());
    }
    @Test void wrongGroupOrAmountOrDirectionNeedsReview() {
        var m=mapper();var service=new AnomalyContextServiceImpl(m);
        var p=payout();p.setGroupId(999L);when(m.findPayoutLinks(2L)).thenReturn(List.of(p));
        var result=service.findContext(12L,2L);
        assertEquals(State.LINK_REQUIRES_REVIEW,result.state());assertNull(result.payoutRequestId());
        assertFalse(result.message().contains("999"));
        p.setGroupId(12L);p.setAmount(new BigDecimal("49999"));
        assertEquals(State.LINK_REQUIRES_REVIEW,service.findContext(12L,2L).state());
        p.setAmount(new BigDecimal("50000"));var t=transaction();t.setTransactionType("DEPOSIT");
        when(m.findTransaction(12L,2L)).thenReturn(t);
        assertEquals(State.LINK_REQUIRES_REVIEW,service.findContext(12L,2L).state());
    }
    @Test void pendingApprovedRejectedAreNotCompletedEvidence() {
        var m=mapper();var p=payout();when(m.findPayoutLinks(2L)).thenReturn(List.of(p));
        for(String status:List.of("PENDING","APPROVED","REJECTED")) {
            p.setStatus(status);
            assertEquals(State.LINK_REQUIRES_REVIEW,new AnomalyContextServiceImpl(m).findContext(12L,2L).state());
        }
    }
    @Test void incompleteOrReversedReviewTimesNeedReview() {
        var m=mapper();var p=payout();when(m.findPayoutLinks(2L)).thenReturn(List.of(p));
        p.setReviewerId(null);
        assertEquals(State.LINK_REQUIRES_REVIEW,new AnomalyContextServiceImpl(m).findContext(12L,2L).state());
        p.setReviewerId(8L);p.setCompletedAt(p.getRequestedAt());
        assertEquals(State.LINK_REQUIRES_REVIEW,new AnomalyContextServiceImpl(m).findContext(12L,2L).state());
        p.setCompletedAt(null);
        assertEquals(State.LINK_REQUIRES_REVIEW,new AnomalyContextServiceImpl(m).findContext(12L,2L).state());
    }
    @Test void duplicateLinksNeverChooseOneArbitrarily() {
        var m=mapper();when(m.findPayoutLinks(2L)).thenReturn(List.of(payout(),payout()));
        assertEquals(State.LINK_REQUIRES_REVIEW,new AnomalyContextServiceImpl(m).findContext(12L,2L).state());
    }
    @Test void missingOrOtherGroupTransactionDoesNotQueryPayouts() {
        var m=mock(AnomalyContextMapper.class);var service=new AnomalyContextServiceImpl(m);
        assertEquals("ANOMALY_CONTEXT_NOT_FOUND",assertThrows(ApiException.class,()->service.findContext(999L,2L)).getCode());
        verify(m,never()).findPayoutLinks(anyLong());
        assertEquals("ANOMALY_CONTEXT_INVALID_ID",assertThrows(ApiException.class,()->service.findContext(0L,2L)).getCode());
    }
    @Test void sqlUsesExplicitTransactionLinkAndScopedGroup() throws Exception {
        var config=new Configuration();
        try(var stream=getClass().getResourceAsStream("/mappers/AnomalyContextMapper.xml")) {
            assertNotNull(stream);new XMLMapperBuilder(stream,config,"AnomalyContextMapper.xml",config.getSqlFragments()).parse();
        }
        String namespace="com.nightstudy.moa.anomaly.mapper.AnomalyContextMapper.";
        String lookup=config.getMappedStatement(namespace+"findTransaction").getBoundSql(Map.of("groupId",12L,"transactionId",2L)).getSql();
        assertTrue(lookup.contains("ga.group_id = ?"));
        String link=config.getMappedStatement(namespace+"findPayoutLinks").getBoundSql(Map.of("transactionId",2L)).getSql();
        assertTrue(link.contains("pr.transaction_id = ?"));assertTrue(link.contains("LIMIT 2"));
        assertFalse(link.contains("pr.amount ="));
    }
}
