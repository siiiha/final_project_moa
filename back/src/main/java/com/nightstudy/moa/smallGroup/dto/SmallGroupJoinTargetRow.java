package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 대상 소모임의 공통 모임 식별자·개설자·정원을 Service에서 검증하는 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinTargetRow {
    private Long smallGroupId;
    private Long groupId;
    private Long creatorMemberId;
    private Integer maxMembers;
    // AI작성: 신청·승인 직전에 잠근 일정으로 종료 여부를 재검사한다.
    private LocalDate meetDate;
}
