package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 목록 카드에 필요한 공개 소모임 정보만 반환하고 내부 집계 정보는 노출하지 않는다.
@Getter
@AllArgsConstructor
public class SmallGroupSummaryResponse {
    private final Long smallGroupId;
    private final Long groupId;
    private final String groupName;
    private final String description;
    private final String regionName;
    private final Integer maxMembers;
    private final long memberCount;
    private final String likeCategory;
    private final LocalDate meetDate;
    private final String location;
}
