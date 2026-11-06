package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

// AI작성: 선택한 공용 계좌는 서버가 모임에서 조회하며 계좌번호·처리자 입력은 받지 않는다.
public record SavingGroupDepositRequest(
        @NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 0) BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{16,64}") String requestKey) {}
