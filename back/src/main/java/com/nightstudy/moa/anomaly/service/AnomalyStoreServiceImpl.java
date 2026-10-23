package com.nightstudy.moa.anomaly.service;

import com.nightstudy.moa.anomaly.dto.*;
import com.nightstudy.moa.anomaly.mapper.AnomalyStorageMapper;
import com.nightstudy.moa.common.exception.ApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// AI작성: DB 미반영 환경 보호를 위해 명시적 활성화 전에는 저장 Bean을 만들지 않는다.
@Service
@ConditionalOnProperty(name="moa.anomaly.persistence.enabled", havingValue="true")
public class AnomalyStoreServiceImpl implements AnomalyStoreService {
    private final AnomalyStorageMapper mapper;
    private final String modelVersion;
    private static final String SCHEMA="moa-anomaly-features-v1";

    public AnomalyStoreServiceImpl(AnomalyStorageMapper mapper,
            @Value("${moa.anomaly.expected-model-version:sha256:f43c3fc790d8a4824f9d5036f732a133714d40e60fa977b917ddcaa8b6863f8d}") String modelVersion) {
        this.mapper=mapper; this.modelVersion=modelVersion;
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public AnalysisWork register(StoredTransaction tx) {
        validate(tx);
        if(mapper.lockAccount(tx.getAccountId())==null) throw error("ANOMALY_ACCOUNT_NOT_FOUND");
        // AI작성: 동일 계좌의 수신을 직렬화하고, 같은 원천 ID의 내용 충돌은 덮어쓰지 않는다.
        StoredTransaction existing=mapper.findSource(tx);
        if(existing!=null) {
            if(!same(existing,tx)) throw error("ANOMALY_SOURCE_CONFLICT");
            return new AnalysisWork(existing.getTransactionId(),1,true,null,null);
        }
        mapper.insertTransaction(tx);
        var request=history(tx);
        String fingerprint=fingerprint(request);
        mapper.insertPending(tx.getTransactionId(),fingerprint);
        return new AnalysisWork(tx.getTransactionId(),1,false,fingerprint,request);
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public String complete(AnalysisWork work, AnomalyResult result) {
        mapper.lockAccount(Long.parseLong(work.request().account_id()));
        StoredTransaction target=mapper.findTransaction(work.transactionId());
        if(target==null) throw error("ANOMALY_TRANSACTION_NOT_FOUND");
        // AI작성: 모델 호출 중 과거 이력이 바뀌면 오래된 점수를 현재 결과로 저장하지 않는다.
        if(!work.fingerprint().equals(fingerprint(history(target)))) {
            mapper.fail(work.transactionId(),work.fingerprint(),"ANOMALY_STALE_HISTORY");
            return "FAILED";
        }
        if(result==null || !modelVersion.equals(result.model_version()) || !SCHEMA.equals(result.feature_schema_version())
                || !String.valueOf(work.transactionId()).equals(result.transaction_id())
                || !String.valueOf(target.getAccountId()).equals(result.account_id())) {
            mapper.fail(work.transactionId(),work.fingerprint(),"ANOMALY_VERSION_OR_ID_MISMATCH");
            return "FAILED";
        }
        // AI작성: 품질 코드는 서버가 정한 영문 식별자만 JSON 배열에 저장한다.
        if(result.quality_flags()==null || result.quality_flags().stream().anyMatch(x->x==null || !x.matches("[a-z0-9_]+")))
            throw error("ANOMALY_INVALID_RESPONSE");
        String flags="["+String.join(",",result.quality_flags().stream().map(x->"\""+x+"\"").toList())+"]";
        if(mapper.complete(work.transactionId(),work.fingerprint(),result,flags)!=1)
            throw error("ANOMALY_STATE_CONFLICT");
        return "COMPLETED";
    }

    @Override
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void fail(AnalysisWork work,String errorCode) {
        // AI작성: 예외 메시지·거래 원문 대신 고정된 분류 코드만 저장한다.
        String safe=errorCode!=null && errorCode.matches("ANOMALY_[A-Z_]+")?errorCode:"ANOMALY_PROCESSING_FAILED";
        mapper.fail(work.transactionId(),work.fingerprint(),safe);
    }

    private AnomalyHistoryRequest history(StoredTransaction target) {
        List<StoredTransaction> rows=mapper.selectHistory(target);
        if(rows.isEmpty()) throw error("ANOMALY_HISTORY_MISSING");
        return new AnomalyHistoryRequest(String.valueOf(target.getAccountId()),String.valueOf(target.getTransactionId()),
            rows.stream().map(t->new AnomalyHistoryRequest.Transaction(String.valueOf(t.getTransactionId()),
                String.valueOf(t.getAccountId()),t.getTransactionType(),decimal(t.getAmount()),
                t.getTransactedAt().atOffset(ZoneOffset.ofHours(9)).toString(),decimal(t.getBalanceAfter()),t.getSourceSequence())).toList());
    }

    // AI작성: 필드 길이를 포함해 경계 충돌을 방지하고 SQL 순서·금액 표기·모델/규격 버전을 고정한다.
    private String fingerprint(AnomalyHistoryRequest request) {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            hash(digest,modelVersion);hash(digest,SCHEMA);hash(digest,request.account_id());hash(digest,request.target_transaction_id());
            for(var t:request.transactions()) {
                hash(digest,t.transaction_id());hash(digest,t.account_id());hash(digest,t.transaction_type());hash(digest,t.amount());
                hash(digest,t.transacted_at());hash(digest,t.balance_after());hash(digest,t.source_sequence());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private void hash(MessageDigest digest,Object value) {
        byte[] bytes=(value==null?"N":"V"+value).getBytes(StandardCharsets.UTF_8);
        digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
    }
    private String decimal(BigDecimal value) { return value==null?null:value.stripTrailingZeros().toPlainString(); }
    private boolean same(StoredTransaction a,StoredTransaction b) {
        return Objects.equals(a.getTransactionType(),b.getTransactionType()) && Objects.equals(decimal(a.getAmount()),decimal(b.getAmount()))
            && Objects.equals(a.getTransactedAt(),b.getTransactedAt()) && Objects.equals(decimal(a.getBalanceAfter()),decimal(b.getBalanceAfter()))
            && Objects.equals(a.getSourceSequence(),b.getSourceSequence());
    }
    private void validate(StoredTransaction tx) {
        if(tx==null || tx.getTransactionId()!=null || tx.getAccountId()==null || tx.getAccountId()<1
                || !("DEPOSIT".equals(tx.getTransactionType()) || "WITHDRAWAL".equals(tx.getTransactionType()))
                || tx.getAmount()==null || tx.getAmount().signum()<=0 || !money(tx.getAmount())
                || (tx.getBalanceAfter()!=null && !money(tx.getBalanceAfter()))
                || tx.getTransactedAt()==null || tx.getTransactedAt().getNano()!=0
                || tx.getSourceProvider()==null || tx.getSourceProvider().isBlank() || tx.getSourceProvider().length()>50
                || tx.getSourceTransactionId()==null || tx.getSourceTransactionId().isBlank() || tx.getSourceTransactionId().length()>128
                || (tx.getSourceSequence()!=null && tx.getSourceSequence()<0)) throw error("ANOMALY_INVALID_INPUT");
        // AI작성: 현재 스키마의 초 정밀도·KST를 임시 계약으로 두고 반올림하지 않는다. DB 담당자 확정 후 조정한다.
    }
    private boolean money(BigDecimal value) {
        return value.stripTrailingZeros().scale()<=2 && value.abs().compareTo(new BigDecimal("9999999999999.99"))<=0;
    }
    private ApiException error(String code) { return new ApiException(HttpStatus.CONFLICT,code,"거래 분석 저장 조건을 확인해 주세요."); }
}
