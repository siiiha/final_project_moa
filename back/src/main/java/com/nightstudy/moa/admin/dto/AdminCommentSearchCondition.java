package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 전체 댓글 목록의 검색·지역·삭제 상태·페이징 조건을 받는다.
public class AdminCommentSearchCondition {
    @Size(max = 100, message = "검색어는 100자 이내로 입력해 주세요.")
    private String keyword;

    @Size(max = 100, message = "지역명은 100자 이내로 입력해 주세요.")
    private String regionName;

    @Pattern(regexp = "VISIBLE|DELETED", message = "댓글 상태를 다시 선택해 주세요.")
    private String status;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    private int offset;
    private int limit;

    public void setKeyword(String keyword) {
        this.keyword = normalize(keyword);
    }

    public void setRegionName(String regionName) {
        this.regionName = normalize(regionName);
    }

    private String normalize(String value) {
        return value == null || value.strip().isEmpty() ? null : value.strip();
    }
}
