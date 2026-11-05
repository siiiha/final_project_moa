package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

// AI작성: 삭제되지 않은 모임의 요청 가능 상태·유형·시작일을 내부 검증에만 사용한다.
@Getter
@Setter
public class SavingGroupPayoutTargetRow {
    private Long groupId;
    private String groupType;
    private String status;
    private LocalDate startDate;
}
