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
    // AI작성: 상위 지역만 선택하면 하위 지역 조건은 null로 유지한다.
    private final String provinceName;
    private final String districtName;
    private final String townName;
    private final String likeCategory;
    @Setter
    private int offset;
    private final int limit;
}
