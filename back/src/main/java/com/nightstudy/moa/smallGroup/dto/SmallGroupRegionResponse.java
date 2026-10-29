package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 소모임 개설 화면이 DB의 활동 지역 기준 데이터를 안전하게 표시하기 위한 응답 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupRegionResponse {
    private Long regionId;
    private String regionName;
}
