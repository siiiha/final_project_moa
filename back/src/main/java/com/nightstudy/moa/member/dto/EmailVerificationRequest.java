package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {

    @NotNull(message = "이메일 인증 요청 정보가 필요합니다.")
    @Positive(message = "올바른 이메일 인증 요청이 아닙니다.")
    private Long verificationRequestId;

    @NotBlank(message = "인증번호를 입력해주세요.")
    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "인증번호는 숫자 6자리로 입력해주세요."
    )
    private String code;
}