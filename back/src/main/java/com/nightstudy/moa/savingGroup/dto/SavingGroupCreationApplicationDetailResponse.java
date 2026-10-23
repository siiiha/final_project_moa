package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 개설 심사 상세는 신청 내용과 처리 결과를 함께 제공한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupCreationApplicationDetailResponse {
    private Long approvalId;
    private Long groupId;
    private String groupName;
    private String applicantName;
    private String regionName;
    private LocalDateTime appliedAt;
    private String status;
    private String groupType;
    private Integer maxMembers;
    private BigDecimal duesAmount;
    private Integer duesDay;
    private String description;
    private String groupImagePath;
    private String rejectReason;
    private LocalDateTime processedAt;
}
