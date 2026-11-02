package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 소모임 관심사 필터가 기준 데이터의 이름을 그대로 전송하도록 반환한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupCategoryResponse {
    private Long interestId;
    private String interestName;
}
