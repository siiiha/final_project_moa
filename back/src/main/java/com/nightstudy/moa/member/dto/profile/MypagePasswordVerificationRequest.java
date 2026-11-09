package com.nightstudy.moa.member.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record MypagePasswordVerificationRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword
) {
}