package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SavingGroupSettingChangeRow {
    private Long settingChangeId;
    private Long groupId;
    private BigDecimal duesAmount;
    private Integer duesDay;
    private LocalDateTime effectiveAt;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;
}
