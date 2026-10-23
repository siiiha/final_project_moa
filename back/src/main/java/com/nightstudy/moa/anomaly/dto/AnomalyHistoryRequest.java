package com.nightstudy.moa.anomaly.dto;
import java.util.List;

// AI작성: Python 계약의 snake_case를 그대로 사용한다. 계좌번호·설명·개인정보는 전송하지 않는다.
public record AnomalyHistoryRequest(String account_id, String target_transaction_id, List<Transaction> transactions) {
    public record Transaction(String transaction_id, String account_id, String transaction_type,
            String amount, String transacted_at, String balance_after, Long source_sequence) {}
}
