package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// AI작성: 기존 목록과 동일하게 1부터 시작하는 페이지 번호만 받는다.
@Getter
@Setter
public class SavingGroupTransactionSearchCondition {
    @NotNull(message = "페이지 번호를 입력해 주세요.")
    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private Integer page = 1;
}
