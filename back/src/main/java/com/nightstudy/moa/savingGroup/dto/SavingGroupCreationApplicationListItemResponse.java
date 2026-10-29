package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 관리자 목록에는 심사에 필요한 최소 신청 정보만 반환한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupCreationApplicationListItemResponse {
    private String approvalId;
    private String groupName;
    private String applicantName;
    private String regionName;
    private LocalDateTime appliedAt;
    private String status;
    private String type;
}
