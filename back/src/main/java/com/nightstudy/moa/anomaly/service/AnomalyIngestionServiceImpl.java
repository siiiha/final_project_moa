package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.StoredTransaction;
import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 등록을 먼저 커밋하고 HTTP는 트랜잭션 밖에서 호출한다. Spring 프록시를 통한 서비스 호출 전제.
@Service
@ConditionalOnProperty(name="moa.anomaly.persistence.enabled",havingValue="true")
public class AnomalyIngestionServiceImpl implements AnomalyIngestionService {
    private final AnomalyStoreService store;
    private final AnomalyService analyzer;
    public AnomalyIngestionServiceImpl(AnomalyStoreService store,AnomalyService analyzer) { this.store=store;this.analyzer=analyzer; }
    @Override
    @Transactional(propagation=Propagation.NEVER)
    public Receipt receive(StoredTransaction transaction) {
        var work=store.register(transaction);
        // AI작성: 중복 수신은 작업 상태를 추측하지 않고 기존 거래를 보존한다. 자동 재시도 아님.
        if(work.duplicate()) return new Receipt(work.transactionId(),true,"DUPLICATE");
        if(work.request().transactions().size()>2000) {
            store.fail(work,"ANOMALY_HISTORY_LIMIT");
            return new Receipt(work.transactionId(),false,"FAILED");
        }
        try {
            String status=store.complete(work,analyzer.analyze(work.request()));
            return new Receipt(work.transactionId(),false,status);
        } catch(RuntimeException exception) {
            String code=exception instanceof ApiException api?api.getCode():"ANOMALY_PROCESSING_FAILED";
            // AI작성: 실패 상태 저장도 실패하면 예외를 전달한다. 원거래는 이미 커밋되어 보존된다.
            store.fail(work,code);
            return new Receipt(work.transactionId(),false,"FAILED");
        }
    }
}
