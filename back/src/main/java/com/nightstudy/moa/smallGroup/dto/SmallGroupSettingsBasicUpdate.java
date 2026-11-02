package com.nightstudy.moa.smallGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: groups 테이블의 소모임 공통 설정 필드만 MyBatis 동적 UPDATE에 전달한다.
@Getter
@AllArgsConstructor
public class SmallGroupSettingsBasicUpdate {
    private final Long groupId;
    private final String groupName;
    private final String description;
    private final Integer maxMembers;
}
