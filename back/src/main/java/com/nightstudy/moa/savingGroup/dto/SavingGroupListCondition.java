package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 검증된 입력을 SQL 바인딩용 조건으로 전달한다.
@Getter
@AllArgsConstructor
public class SavingGroupListCondition {
    private final String keywordPattern;
    private final String regionName;
    private final long offset;
    private final int limit;
}
