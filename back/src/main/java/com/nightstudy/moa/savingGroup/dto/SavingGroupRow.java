package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: MyBatis 조회 결과와 계원 집계 연결에 필요한 내부 모임 ID를 받는다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupRow {
    private Long savingGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private Integer maxMembers;
    private BigDecimal duesAmount;
    private Integer duesDay;
    private String status;
}
