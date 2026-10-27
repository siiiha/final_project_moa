package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 현재 비밀번호와 수정할 회원정보를 전달받습니다.
@Getter
@Setter
public class MemberProfileUpdateRequest {

    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;

    @Size(min = 2, max = 12, message = "닉네임은 2~12자로 입력해주세요.")
    private String nickname;

    private List<
            @NotNull(message = "관심사 번호는 비어 있을 수 없습니다.")
            @Positive(message = "관심사 번호는 양수여야 합니다.")
                    Long
            > interestIds; // 생략하면 유지하고, 빈 배열 [] 이면 관심사 전부 해제
}