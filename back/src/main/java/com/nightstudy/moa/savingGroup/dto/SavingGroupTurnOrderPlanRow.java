package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// AI작성: DB 담당자가 적용하는 예약 테이블과 모임 조회의 내부 매핑이다.
@Getter @Setter
public class SavingGroupTurnOrderPlanRow {
    private Long turnOrderPlanId;
    private Long savingGroupId;
    private Long groupId;
    private String groupType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate effectiveMonth;
    private Integer startRoundNumber;
    private Long assignedBy;
}
