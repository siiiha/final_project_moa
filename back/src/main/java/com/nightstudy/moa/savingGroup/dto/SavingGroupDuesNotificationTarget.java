package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;

// AI작성: 회비 납부 알림에 필요한 수신자·공개 계모임 번호·금액만 전달한다.
public record SavingGroupDuesNotificationTarget(
        Long receiverId,
        Long savingGroupId,
        BigDecimal duesAmount
) {}
