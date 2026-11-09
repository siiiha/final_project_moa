package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// AI작성: 기존 신청 목록의 상태·1부터 시작하는 페이지를 사용하며 크기는 서버에서 10개로 고정한다.
@Getter
@Setter
public class SavingGroupPayoutRequestSearchCondition {
    @Pattern(regexp = "PENDING|APPROVED|REJECTED|COMPLETED", message = "요청 상태가 올바르지 않습니다.")
    private String status;
    @NotNull(message = "페이지 번호를 입력해 주세요.")
    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private Integer page = 1;
}
