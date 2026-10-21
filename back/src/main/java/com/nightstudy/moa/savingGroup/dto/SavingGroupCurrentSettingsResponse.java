package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class SavingGroupCurrentSettingsResponse {
    private String groupName;
    private String description;
    private Integer maxMembers;
    private long approvedMemberCount;
    private BigDecimal duesAmount;
    private Integer duesDay;
}
