package com.nightstudy.moa.anomaly.dto;
import java.math.BigDecimal;
import lombok.Data;

// AI작성: 업무 문맥 확인에 필요한 기존 거래 컬럼만 조회한다. 미반영 source/잔액 컬럼에 의존하지 않는다.
@Data
public class BusinessContextTransaction {
    private Long transactionId;
    private Long groupId;
    private String transactionType;
    private BigDecimal amount;
}
