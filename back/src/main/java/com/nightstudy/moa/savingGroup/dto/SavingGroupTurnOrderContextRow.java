package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

// AI작성: 공개 계모임 ID를 내부 모임 ID로 해석하고 유형·상태·기본 회비를 검증한다.
@Getter
@Setter
public class SavingGroupTurnOrderContextRow {
    private Long groupId;
    private String groupType;
    private String status;
    private BigDecimal duesAmount;
}
