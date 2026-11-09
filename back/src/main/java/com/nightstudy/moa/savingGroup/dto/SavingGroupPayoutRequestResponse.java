package com.nightstudy.moa.savingGroup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

// AI작성: 유형별 실제 컬럼만 노출한다. 이름은 확정 정책에 따라 원문이며 계좌·거래 ID·증빙 URL은 포함하지 않는다.
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavingGroupPayoutRequestResponse {
    private String requestId;
    private String requesterName;
    private BigDecimal amount;
    private String status;
    private LocalDateTime requestedAt;
    private Integer roundNumber;
    private String title;
    private Boolean hasEvidence;
    private String reviewerName;
    private LocalDateTime reviewedAt;
}
