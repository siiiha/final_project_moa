package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// AI작성: 고정 지정(true)·해제(false)를 명시하며 처리 회원과 모임 정보는 요청 본문으로 받지 않는다.
@Getter
@Setter
public class SavingGroupNoticePinRequest {
    @NotNull(message = "공지 고정 여부는 필수입니다.")
    private Boolean pinned;
}
