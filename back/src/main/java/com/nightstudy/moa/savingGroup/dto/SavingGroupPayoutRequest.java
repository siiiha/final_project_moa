package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 친목계 입력만 받으며 필수 여부는 서버가 조회한 유형에 따라 Service에서 검증한다.
@Getter
@Setter
public class SavingGroupPayoutRequest {
    @DecimalMin(value = "1", message = "금액은 1원 이상이어야 합니다.")
    @Digits(integer = 13, fraction = 0, message = "금액은 원 단위 정수로 입력해주세요.")
    private BigDecimal amount;
    @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
    private String title;
}
