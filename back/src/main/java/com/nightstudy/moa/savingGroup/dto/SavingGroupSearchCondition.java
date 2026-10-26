package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 목록 요청만 검증하며 바인딩 시 검색어를 정규화한다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupSearchCondition {
    @Size(max = 20, message = "검색어는 20자 이내로 입력해 주세요.")
    private String keyword;

    @Pattern(regexp = "0[1-9]|10", message = "지역을 다시 선택해 주세요.")
    private String regionCode;

    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    // AI작성: Validation보다 먼저 앞뒤 공백을 제거하고 공백 검색을 해제한다.
    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
    }
}
