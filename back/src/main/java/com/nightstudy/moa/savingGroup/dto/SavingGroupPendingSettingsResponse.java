package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingGroupPendingSettingsResponse {
    private final BigDecimal duesAmount;
    private final Integer duesDay;
    private final LocalDateTime effectiveAt;
    private final String status;
}
