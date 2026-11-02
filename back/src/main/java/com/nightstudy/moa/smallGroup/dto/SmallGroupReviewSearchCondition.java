package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 후기 목록은 검색·필터·정렬·크기 입력 없이 페이지 번호만 받는다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupReviewSearchCondition {
    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;
}
