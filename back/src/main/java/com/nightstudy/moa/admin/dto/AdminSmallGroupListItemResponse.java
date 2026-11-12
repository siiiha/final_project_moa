package com.nightstudy.moa.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 소모임 목록에서만 모임장과 운영 상태를 함께 제공한다.
public class AdminSmallGroupListItemResponse {
    private Long smallGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private Integer maxMembers;
    private String likeCategory;
    private LocalDate meetDate;
    private String location;
    private String creatorName;
    private String creatorNickname;
    private String status;
    private long memberCount;
    // AI작성: 관리자 소모임 목록에서 개설일을 표시하기 위한 공통 모임 생성 시각이다.
    private LocalDateTime createdAt;
}
