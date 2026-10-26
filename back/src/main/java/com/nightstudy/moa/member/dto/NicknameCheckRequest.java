package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameCheckRequest {

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 12, message = "닉네임은 12자 이내로 입력해주세요.")
    private String nickname;
}
