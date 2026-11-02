package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 계주가 삭제 심사를 신청할 때 보존할 필수 사유를 받는다.
public class SavingGroupDeletionRequest {
    @NotBlank(message = "삭제 사유는 필수입니다.")
    @Size(max = 500, message = "삭제 사유는 500자 이하여야 합니다.")
    private String reason;
}
