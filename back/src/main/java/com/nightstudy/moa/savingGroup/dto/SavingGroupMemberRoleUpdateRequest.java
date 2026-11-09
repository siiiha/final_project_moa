package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// AI작성: 계주 이전을 허용하지 않으며 기존 DB 역할 중 MEMBER·MANAGER만 요청할 수 있다.
@Getter
@Setter
public class SavingGroupMemberRoleUpdateRequest {
    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "MEMBER|MANAGER", message = "계원 또는 회계 역할만 지정할 수 있습니다.")
    private String role;
}
