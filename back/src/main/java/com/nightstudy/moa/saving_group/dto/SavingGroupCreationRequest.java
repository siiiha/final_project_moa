package com.nightstudy.moa.saving_group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@Getter
public class SavingGroupCreationRequest {
    private Long regionId;        // 활동 지역 ID
    private Long memberId;     // 모임장(계주) 회원 ID
    private String groupType;       // 계모임 종류 (순번계 or 친목계)
    private String groupName;       // 계모임 이름
    private String description;     // 계모임 소개 글
    private int maxMembers;         // 모임원 정원
    private BigDecimal duesAmount;  // 정기 회비
    private LocalDate duesDay;      // 정기 회부 납부일
}
