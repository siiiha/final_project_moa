package com.nightstudy.moa.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 계모임 목록 한 행에 표시할 정보를 담는다.
public class AdminSavingGroupListItemResponse {
    private Long savingGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private Integer maxMembers;
    private Integer memberCount;
    private BigDecimal duesAmount;
    private Integer duesDay;
    private String status;
    private LocalDateTime createdAt;
}
