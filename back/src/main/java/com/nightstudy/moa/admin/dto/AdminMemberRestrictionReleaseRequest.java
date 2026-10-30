package com.nightstudy.moa.admin.dto;

import com.nightstudy.moa.common.restriction.MemberRestrictionScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자가 기능별 회원 제한을 해제할 때 범위와 필수 사유를 받는다.
public class AdminMemberRestrictionReleaseRequest {
    @NotNull(message = "제한 해제 범위는 필수입니다.")
    private MemberRestrictionScope restrictionScope;

    @NotBlank(message = "제한 해제 사유는 필수입니다.")
    @Size(max = 500, message = "제한 해제 사유는 500자 이하여야 합니다.")
    private String reason;
}
