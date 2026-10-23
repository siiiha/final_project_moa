package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.*;

// AI작성: 짧은 DB 트랜잭션 경계를 모델 HTTP 호출과 분리한다.
public interface AnomalyStoreService {
    AnalysisWork register(StoredTransaction transaction);
    String complete(AnalysisWork work, AnomalyResult result);
    void fail(AnalysisWork work, String errorCode);
}
