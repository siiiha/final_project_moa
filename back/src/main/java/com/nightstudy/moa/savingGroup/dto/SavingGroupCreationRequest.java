package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 계모임 개설 요청 DTO를 savingGroup 도메인 패키지로 통합한다.
@NoArgsConstructor
@Getter
@Setter
public class SavingGroupCreationRequest {
    private String groupName;
    private Long groupId;
    private Long regionId;
    private Long memberId;
    private int maxMembers;
    private String description;
    private String groupType;
    private BigDecimal duesAmount;
    private LocalDate duesDay;
    private MultipartFile groupImage;
    private String groupImagePath;
}
