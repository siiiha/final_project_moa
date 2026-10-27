package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailCheckRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식으로 입력해주세요.")
    @Size(max = 40, message = "이메일은 40자 이내로 입력해주세요.")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "공백 없이 올바른 이메일 주소를 입력해주세요."
    )
    private String email;

    // 기존 회원가입 요청은 SIGNUP을 기본값으로 사용
    @NotBlank(message = "이메일 인증 목적을 입력해주세요.")
    @Pattern(
            regexp = "^(SIGNUP|FIND_ACCOUNT|RESET_PASSWORD)$",
            message = "이메일 인증 목적이 올바르지 않습니다."
    )
    private String purpose = "SIGNUP";
}