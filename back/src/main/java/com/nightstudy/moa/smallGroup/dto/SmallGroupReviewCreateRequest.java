package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 후기 입력값만 받고 작성자와 소모임 식별자는 인증 정보와 경로에서 결정한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupReviewCreateRequest {
    @NotNull(message = "별점은 필수입니다.")
    @DecimalMin(value = "0.0", message = "별점은 0점 이상이어야 합니다.")
    @DecimalMax(value = "5.0", message = "별점은 5점 이하여야 합니다.")
    private BigDecimal rating;

    @NotBlank(message = "코멘트를 입력해주세요.")
    @Size(min = 1, max = 30, message = "코멘트는 1자 이상 30자 이하로 입력해주세요.")
    private String content;

    // AI작성: DB 반올림 전에 원본 별점의 0.5 단위를 검증하며 누락은 NotNull에서 처리한다.
    @AssertTrue(message = "별점은 0.5점 단위로 입력해주세요.")
    public boolean isRatingStepValid() {
        return rating == null || rating.remainder(new BigDecimal("0.5")).signum() == 0;
    }
}
