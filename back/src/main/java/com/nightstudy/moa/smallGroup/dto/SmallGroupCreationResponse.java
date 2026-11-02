package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 생성 성공 후 클라이언트가 확인할 수 있는 소모임 기본 정보 응답이다.
@Getter
@AllArgsConstructor
public class SmallGroupCreationResponse {
    private final Long smallGroupId;
    private final Long groupId;
    private final String groupName;
    private final String regionName;
    private final String likeCategory;
    private final String description;
    private final Integer maxMembers;
    private final LocalDate meetDate;
    private final String location;
    private final String memberRole;
}
