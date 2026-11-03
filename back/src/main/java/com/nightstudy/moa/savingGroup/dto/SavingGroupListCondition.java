package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// AI작성: 검증된 입력을 SQL 바인딩용 조건으로 전달한다.
@Getter
@AllArgsConstructor
public class SavingGroupListCondition {
    private final String keywordPattern;
    private final String regionName;
    // AI작성: 하위 단계가 null이면 해당 상위 지역 전체를 조회한다.
    private final String provinceName;
    private final String districtName;
    private final String townName;
    @Setter
    private int offset;
    private final int limit;
}
