package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// AI작성: 검증된 관리자 목록 조건을 MyBatis 바인딩용으로 전달한다.
@Getter
@AllArgsConstructor
public class SavingGroupCreationApplicationListCondition {
    private final String keywordPattern;
    private final String status;
    @Setter
    private int offset;
    private final int limit;
}
