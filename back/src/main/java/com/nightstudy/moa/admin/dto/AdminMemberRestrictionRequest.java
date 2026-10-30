package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자가 회원에게 적용할 기능 범위, 제한 기간과 필수 사유를 받는다.
public class AdminMemberRestrictionRequest {
    @NotNull(message = "이용 제한 범위는 필수입니다.")
    private MemberRestrictionScope restrictionScope;

    @NotBlank(message = "이용 제한 기간은 필수입니다.")
    @Pattern(
            regexp = "7_DAYS|30_DAYS|PERMANENT",
            message = "이용 제한은 7_DAYS, 30_DAYS, PERMANENT 중 하나여야 합니다."
    )
    private String restrictionType;

    @NotBlank(message = "이용 제한 사유는 필수입니다.")
    @Size(max = 500, message = "이용 제한 사유는 500자 이하여야 합니다.")
    private String reason;
}
