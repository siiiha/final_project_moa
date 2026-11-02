package com.nightstudy.moa.smallGroup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 목록 페이지에 포함된 소모임별 승인 계원 수를 매핑한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupMemberCount {
    private Long groupId;
    private long memberCount;
}
