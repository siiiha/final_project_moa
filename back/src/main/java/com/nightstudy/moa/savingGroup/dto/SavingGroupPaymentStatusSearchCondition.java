package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

// AI작성: 기존 페이지 검증을 재사용하고 조회 월을 YYYY-MM으로 제한한다.
@Getter
@Setter
public class SavingGroupPaymentStatusSearchCondition extends SavingGroupTransactionSearchCondition {
    @NotBlank(message = "조회 월을 입력해 주세요.")
    @Pattern(regexp = "(?!0000)[0-9]{4}-(0[1-9]|1[0-2])", message = "조회 월은 YYYY-MM 형식이어야 합니다.")
    private String month;
}
