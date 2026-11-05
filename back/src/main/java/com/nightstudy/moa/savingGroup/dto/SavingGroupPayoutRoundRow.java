package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

// AI작성: 회차 생성 시 확정한 산정값을 보존하며 요청 시 현재 회비·인원으로 재계산하지 않는다.
@Getter
@Setter
public class SavingGroupPayoutRoundRow {
    private Long roundId;
    private Integer roundNumber;
    private BigDecimal baseDuesAmount;
    private Integer approvedMemberCount;
    private Integer previousPaidMemberCount;
    private BigDecimal additionalDuesAmount;
    private BigDecimal payoutAmount;
}
