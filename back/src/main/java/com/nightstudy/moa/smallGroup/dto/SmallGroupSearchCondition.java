package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 계모임과 같은 검색·필터·페이지 입력을 검증한다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupSearchCondition {
    @Size(max = 20, message = "검색어는 20자 이내로 입력해 주세요.")
    private String keyword;

    @Pattern(regexp = "0[1-9]|10", message = "지역을 다시 선택해 주세요.")
    private String regionCode;

    @Size(max = 50, message = "관심사는 50자 이내로 입력해 주세요.")
    private String likeCategory;

    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    // AI작성: 계모임 목록과 동일하게 공백 검색을 해제하고 앞뒤 공백을 제거한다.
    public void setKeyword(String keyword) {
        this.keyword = normalize(keyword);
    }

    public void setLikeCategory(String likeCategory) {
        this.likeCategory = normalize(likeCategory);
    }

    private String normalize(String value) {
        return value == null || value.strip().isEmpty() ? null : value.strip();
    }
}
