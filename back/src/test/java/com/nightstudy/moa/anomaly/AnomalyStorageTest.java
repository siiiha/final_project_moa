package com.nightstudy.moa.anomaly;

import com.nightstudy.moa.anomaly.dto.*;
import com.nightstudy.moa.anomaly.mapper.AnomalyStorageMapper;
import com.nightstudy.moa.anomaly.service.*;
import com.nightstudy.moa.common.exception.ApiException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI작성: 실제 DB 실행 검증이 아니다. SQL 파싱·서비스 분기·트랜잭션 선언만 확인한다.
class AnomalyStorageTest {
    static final String VERSION="sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d";
    StoredTransaction tx() {
        var t=new StoredTransaction();t.setAccountId(10013L);t.setTransactionType("WITHDRAWAL");
        t.setAmount(new BigDecimal("50000.00"));t.setBalanceAfter(new BigDecimal("1000000.00"));
        t.setTransactedAt(LocalDateTime.of(2026,9,18,20,8));t.setSourceProvider("MOCK");
        t.setSourceTransactionId("synthetic-2");return t;
    }
    AnomalyResult result() {
        return new AnomalyResult("10013","2","2026-09-18T20:08:00+09:00","WITHDRAWAL",0.7,0.6,true,
            "not a probability",VERSION,"moa-anomaly-features-v1",true,List.of("same_direction_history_under_5"),true);
    }
    AnomalyStorageMapper mapper(StoredTransaction t) {
        var m=mock(AnomalyStorageMapper.class);
        when(m.lockAccount(10013L)).thenReturn(10013L);
        when(m.insertTransaction(t)).thenAnswer(inv->{t.setTransactionId(2L);return 1;});
        when(m.selectHistory(any())).thenReturn(List.of(t));
        when(m.findTransaction(2L)).thenReturn(t);
        when(m.complete(anyLong(),anyString(),any(),anyString())).thenReturn(1);
        return m;
    }
    @Test void registersPendingAndCompletes() {
        var t=tx();var m=mapper(t);var store=new AnomalyStoreServiceImpl(m,VERSION);
        var work=store.register(t);
        assertEquals(64,work.fingerprint().length());
        assertNull(work.request().transactions().get(0).source_sequence());
        assertEquals("1000000",work.request().transactions().get(0).balance_after());
        assertEquals("COMPLETED",store.complete(work,result()));
        var order=inOrder(m);order.verify(m).insertTransaction(t);
        order.verify(m).insertPending(2L,work.fingerprint());
        verify(m).complete(2L,work.fingerprint(),result(),"[\"same_direction_history_under_5\"]");
    }
    @Test void duplicateDoesNotInsertAndConflictDoesNotOverwrite() {
        var incoming=tx();var existing=tx();existing.setTransactionId(2L);
        var m=mapper(incoming);when(m.findSource(incoming)).thenReturn(existing);
        var store=new AnomalyStoreServiceImpl(m,VERSION);
        assertTrue(store.register(incoming).duplicate());
        verify(m,never()).insertTransaction(any());
        existing.setAmount(new BigDecimal("1.00"));
        assertEquals("ANOMALY_SOURCE_CONFLICT",assertThrows(ApiException.class,()->store.register(incoming)).getCode());
    }
    @Test void staleHistoryDoesNotStoreOldScore() {
        var t=tx();var m=mapper(t);var store=new AnomalyStoreServiceImpl(m,VERSION);
        var work=store.register(t);t.setBalanceAfter(new BigDecimal("999999.00"));
        assertEquals("FAILED",store.complete(work,result()));
        verify(m).fail(2L,work.fingerprint(),"ANOMALY_STALE_HISTORY");
        verify(m,never()).complete(anyLong(),anyString(),any(),anyString());
    }
    @Test void networkFailureKeepsRegisteredTradeAndMarksFailure() {
        var store=mock(AnomalyStoreService.class);var analyzer=mock(AnomalyService.class);
        var work=new AnalysisWork(2L,1,false,"fingerprint",new AnomalyHistoryRequest("10013","2",List.of()));
        when(store.register(any())).thenReturn(work);
        when(analyzer.analyze(any())).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"ANOMALY_CONNECTION_FAILED","private"));
        assertEquals("FAILED",new AnomalyIngestionServiceImpl(store,analyzer).receive(tx()).status());
        var order=inOrder(store,analyzer);order.verify(store).register(any());order.verify(analyzer).analyze(any());
        order.verify(store).fail(work,"ANOMALY_CONNECTION_FAILED");
        verify(store,never()).complete(any(),any());
    }
    @Test void duplicateSkipsModel() {
        var store=mock(AnomalyStoreService.class);var analyzer=mock(AnomalyService.class);
        when(store.register(any())).thenReturn(new AnalysisWork(2L,1,true,null,null));
        assertEquals("DUPLICATE",new AnomalyIngestionServiceImpl(store,analyzer).receive(tx()).status());
        verifyNoInteractions(analyzer);
    }
    @Test void transactionBoundariesDeclared() throws Exception {
        assertEquals(Propagation.NEVER,AnomalyIngestionServiceImpl.class.getMethod("receive",StoredTransaction.class)
            .getAnnotation(Transactional.class).propagation());
        assertEquals(Propagation.REQUIRES_NEW,AnomalyStoreServiceImpl.class.getMethod("register",StoredTransaction.class)
            .getAnnotation(Transactional.class).propagation());
    }
    @Test void mapperXmlParsesAndRecordParametersResolve() throws Exception {
        var configuration=new Configuration();
        try(var stream=getClass().getResourceAsStream("/mappers/AnomalyStorageMapper.xml")) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream,configuration,"AnomalyStorageMapper.xml",configuration.getSqlFragments()).parse();
        }
        String name="com.nightstudy.moa.anomaly.mapper.AnomalyStorageMapper.complete";
        var args=Map.of("transactionId",2L,"fingerprint","test","result",result(),"qualityFlags","[]");
        var sql=configuration.getMappedStatement(name).getBoundSql(args);
        assertTrue(sql.getSql().contains("status = 'PENDING'"));
        for(var parameter:sql.getParameterMappings())
            assertNotNull(configuration.newMetaObject(args).getValue(parameter.getProperty()),parameter.getProperty());
    }
}
