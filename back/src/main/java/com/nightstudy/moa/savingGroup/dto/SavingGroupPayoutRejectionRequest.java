package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 기존 심사 DTO 형식으로 거절 사유만 받고 처리자는 인증 정보에서 결정한다.
@Getter
@Setter
public class SavingGroupPayoutRejectionRequest {
    @NotBlank(message = "거절 사유를 입력해 주세요.")
    @Size(max = 500, message = "거절 사유는 500자 이내로 입력해 주세요.")
    private String rejectReason;
}
