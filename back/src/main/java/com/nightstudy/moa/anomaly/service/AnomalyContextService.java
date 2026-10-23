package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.TransactionBusinessContext;

// AI작성: 모임 권한 확인을 마친 내부 호출자가 사용한다. 이 인터페이스 자체는 회원 권한을 판정하지 않는다.
public interface AnomalyContextService {
    TransactionBusinessContext findContext(long groupId, long transactionId);
}
