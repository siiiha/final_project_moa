package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MemberSignupRequest {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식으로 입력해주세요.")
    @Size(max = 40, message = "이메일은 40자 이내로 입력해주세요.")
    @Pattern(
            regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",
            message = "공백 없이 올바른 이메일 주소를 입력해주세요."
    )
    private String email;

    @NotBlank(message = "이메일 인증을 완료해주세요.")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]{43}$",
            message = "이메일 인증 정보가 올바르지 않습니다."
    )
    private String emailVerificationToken;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 10, message = "이름은 10자 이내로 입력해주세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 12, message = "닉네임은 12자 이내로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Size(max = 20, message = "전화번호는 20자 이내로 입력해주세요.")
    private String phone;

    //문자열이 아니라 날짜를 담는 필드라서 @NotBlank 사용 x
    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일은 오늘보다 이전 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotBlank(message = "성별을 선택해주세요.")
    @Pattern(
            regexp = "MALE|FEMALE",
            message = "성별을 다시 선택해주세요."
    )
    private String gender;

    @NotNull(message = "지역을 선택해주세요.")
    @Positive(message = "지역을 다시 선택해주세요.")
    private Long regionId;

    @NotNull(message = "약관 동의 여부를 확인해주세요.")
    private List<@NotNull @Positive Long> agreedTermIds;

    @NotNull(message = "관심사 선택 정보를 확인해주세요.")
    @Size(max = 3, message = "관심사는 최대 3개까지 선택할 수 있습니다.")
    private List<@NotNull @Positive Long> interestIds;
}