package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis 상세 조회 결과를 Service의 공개 응답 조합에만 사용한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupDetailRow {
    private Long smallGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private String likeCategory;
    private LocalDate meetDate;
    private String location;
    private Integer maxMembers;
    private String creatorNickname;
    private LocalDateTime createdAt;
}
