package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 계모임과 소모임의 관리자 제한·해제 사유를 공통으로 검증한다.
public class AdminGroupRestrictionRequest {
    @NotBlank(message = "처리 사유는 필수입니다.")
    @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
    private String reason;
}
