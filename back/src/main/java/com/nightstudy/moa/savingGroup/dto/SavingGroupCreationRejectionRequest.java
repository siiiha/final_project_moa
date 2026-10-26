package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 반려 사유는 신청자 안내와 심사 이력 확인을 위해 필수로 저장한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupCreationRejectionRequest {
    @NotBlank(message = "반려 사유를 입력해 주세요.")
    @Size(max = 255, message = "반려 사유는 255자 이내로 입력해 주세요.")
    private String rejectReason;
}
