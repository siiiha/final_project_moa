package com.nightstudy.moa.anomaly.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// AI작성: 사용자 첨부 group_transaction 정의에 대응한다. 원천 순번은 스키마에 없어 저장하지 않는다.
@Data
public class StoredTransaction {
    private Long transactionId;
    private Long accountId;
    private String transactionType;
    private BigDecimal amount;
    private LocalDateTime transactedAt;
    private BigDecimal balanceAfter;
    private String sourceProvider;
    private String sourceTransactionId;
    // AI작성: 같은 시각의 원천 순서를 추정하지 않는다. Python 요청에는 null을 전달한다.
}
