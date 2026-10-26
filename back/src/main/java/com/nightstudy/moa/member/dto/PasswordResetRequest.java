package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequest extends EmailVerificationTokenRequest {

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Size(
            min = 8,
            max = 20,
            message = "새 비밀번호는 8자 이상 20자 이하로 입력해주세요."
    )
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    @Size(max=20, message = "비밀번호 확인은 20자 이하로 입력해주세요.")
    private String newPasswordConfirm;

    // 비밀번호와 확인 입력 일치 여부를 서버에서도 검사
    @JsonIgnore
    @AssertTrue(message = "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return newPassword != null
                && newPassword.equals(newPasswordConfirm);
    }

}
