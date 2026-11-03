package com.nightstudy.moa.member.dto.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberJoinedGroupResponse {

    private Long groupId;

    // saving: 계모임, small: 소모임
    private String groupType;

    // 모임 상세 페이지로 이동할 때 사용하는 ID
    private Long detailId;

    private String title;
    private String description;
    private String regionName;
    private Long memberCount;
    private String imageUrl;
}
