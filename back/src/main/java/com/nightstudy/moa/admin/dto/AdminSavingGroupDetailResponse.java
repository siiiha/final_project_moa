package com.nightstudy.moa.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 계모임 상세 화면에 필요한 모임·계주·회비 정보를 담는다.
public class AdminSavingGroupDetailResponse {
    private Long savingGroupId;
    private Long groupId;
    private String groupName;
    private String description;
    private String regionName;
    private String savingGroupType;
    private Integer maxMembers;
    private Integer memberCount;
    private BigDecimal duesAmount;
    private Integer duesDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String groupImage;
    private Long ownerMemberId;
    private String ownerNickname;
    private LocalDateTime createdAt;
}
