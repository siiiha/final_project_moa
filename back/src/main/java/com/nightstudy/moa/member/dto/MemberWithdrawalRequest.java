package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberWithdrawalRequest {

    @NotBlank(message =  "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    @AssertTrue(message = "회원탈퇴 안내사항에 동의해주세요.")
    private boolean agreed;
}
