package com.nightstudy.moa.smallGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// AI작성: 검증·정규화된 검색 조건을 MyBatis 목록 SQL에 전달한다.
@Getter
@AllArgsConstructor
public class SmallGroupListCondition {
    private final String keywordPattern;
    private final String regionName;
    private final String likeCategory;
    @Setter
    private int offset;
    private final int limit;
}
