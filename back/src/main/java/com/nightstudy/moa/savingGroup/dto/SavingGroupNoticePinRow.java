package com.nightstudy.moa.savingGroup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// AI작성: Service가 잠금 조회한 공지의 존재 여부와 현재 고정 상태를 확인하는 내부 DTO다.
@Getter
@AllArgsConstructor
public class SavingGroupNoticePinRow {
    private final Long noticeId;
    private final Boolean pinned;
}
