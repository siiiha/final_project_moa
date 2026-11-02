package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 제한 처리 중 잠근 소모임의 식별자와 현재 DB 상태를 전달한다.
public class AdminSmallGroupRestrictionTarget {
    private Long smallGroupId;
    private String status;
}
