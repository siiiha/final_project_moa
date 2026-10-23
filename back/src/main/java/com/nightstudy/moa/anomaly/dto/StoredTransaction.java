package com.nightstudy.moa.anomaly.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// AI작성: 제안 group_transaction 컬럼에 대응한다. 외부 입력을 직접 바인딩하는 공개 DTO가 아니다.
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
    private Long sourceSequence;
}
