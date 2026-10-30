package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityVerificationRequest {

    @NotBlank(message = "본인인증 요청 번호가 필요합니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{1,128}$",
            message = "본인인증 요청 번호가 올바르지 않습니다."
    )
    private String identityVerificationId;
}
