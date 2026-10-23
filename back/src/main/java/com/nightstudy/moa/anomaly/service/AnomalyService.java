package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.AnomalyHistoryRequest;
import com.nightstudy.moa.anomaly.dto.AnomalyResult;

// AI작성: 거래 저장 서비스가 호출할 내부 인터페이스. 공개 Controller와 DB 저장은 별도 구현한다.
public interface AnomalyService {
    AnomalyResult analyze(AnomalyHistoryRequest request);
}
