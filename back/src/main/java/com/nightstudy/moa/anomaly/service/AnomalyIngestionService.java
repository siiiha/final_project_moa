package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.StoredTransaction;

// AI작성: 승인된 내부 수집 어댑터에서만 호출한다. 공개 입력·은행 호출 기능이 아니다.
public interface AnomalyIngestionService {
    Receipt receive(StoredTransaction transaction);
    record Receipt(long transactionId, boolean duplicate, String status) {}
}
