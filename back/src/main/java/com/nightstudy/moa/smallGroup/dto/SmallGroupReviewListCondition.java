package com.nightstudy.moa.smallGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// AI작성: Service에서 검증한 대상 소모임과 페이지 위치만 SQL에 전달하고 조회 크기는 8개로 고정한다.
@Getter
@AllArgsConstructor
public class SmallGroupReviewListCondition {
    private final Long smallGroupId;
    @Setter
    private int offset;
    private final int limit = 8;
}
