package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 투표 목록 조회 전에 삭제되지 않은 소모임과 공통 모임 식별자를 확인하는 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupVoteListTargetRow {
    private Long smallGroupId;
    private Long groupId;
}
