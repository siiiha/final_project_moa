package com.nightstudy.moa.savingGroup.dto;

import lombok.Getter;
import lombok.Setter;

// AI작성: 유형별 요청 테이블의 심사에 필요한 값만 읽으며 영수증 원문은 반환하지 않는다.
@Getter
@Setter
public class SavingGroupPayoutDecisionRow {
    private Long groupId;
    private String groupType;
    private String status;
    private Long ownerMemberId;
    private Long requesterId;
    private Integer roundNumber;
    private Integer sequenceNumber;
    private Boolean hasEvidence;
}
