package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis 목록 조회 결과와 승인 계원 집계를 연결할 내부 모임 ID를 보관한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupRow {
    private Long smallGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private Integer maxMembers;
    private String likeCategory;
    private LocalDate meetDate;
    private String location;
}
