package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: 승인된 개설자가 설정 화면에서 현재 수정 가능한 소모임 정보를 조회할 때 반환한다.
@Getter
@AllArgsConstructor
public class SmallGroupSettingsResponse {
    private final String groupName;
    private final String likeCategory;
    private final String description;
    private final Integer maxMembers;
    private final LocalDate meetDate;
    private final String location;
    // AI작성: 정원 축소 전 현재 승인 OWNER와 MEMBER 수를 화면과 서버에서 함께 검증하는 기준값이다.
    private final long approvedMemberCount;
}
