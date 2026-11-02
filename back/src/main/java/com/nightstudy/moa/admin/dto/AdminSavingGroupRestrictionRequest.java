package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 기간 없이 계모임을 제한하거나 해제할 때 보존할 관리자 사유를 받는다.
public class AdminSavingGroupRestrictionRequest {
    @NotBlank(message = "처리 사유는 필수입니다.")
    @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
    private String reason;
}
