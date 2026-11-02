package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRegionUpdateRequest {

    @NotNull(message = "활동 지역을 선택해주세요.")
    @Positive(message = "올바른 활동 지역을 선택해주세요.")
    private Long regionId;
}
