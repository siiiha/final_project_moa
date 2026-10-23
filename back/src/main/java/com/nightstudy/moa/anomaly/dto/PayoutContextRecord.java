package com.nightstudy.moa.anomaly.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

// AI작성: 거래와 직접 FK로 연결된 지급 요청의 최소 내부 정보. 검토자 ID는 외부 결과에 노출하지 않는다.
@Data
public class PayoutContextRecord {
    private Long payoutRequestId;
    private Long groupId;
    private String status;
    private BigDecimal amount;
    private Long reviewerId;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime completedAt;
}
