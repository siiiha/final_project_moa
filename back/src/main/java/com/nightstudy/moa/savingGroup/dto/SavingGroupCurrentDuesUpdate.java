package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SavingGroupCurrentDuesUpdate {
    private final Long savingGroupId;
    private final BigDecimal duesAmount;
    private final Integer duesDay;
}
