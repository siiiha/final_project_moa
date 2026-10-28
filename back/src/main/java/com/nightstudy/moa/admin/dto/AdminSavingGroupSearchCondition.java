package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 계모임 목록의 검색·상태·지역·페이징 조건을 받는다.
public class AdminSavingGroupSearchCondition {
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도"
    );

    @Size(max = 20, message = "검색어는 20자 이내로 입력해 주세요.")
    private String keyword;
    private String keywordPattern;

    @Pattern(regexp = "0[1-9]|10", message = "지역을 다시 선택해 주세요.")
    private String regionCode;
    private String regionName;

    // AI작성: 현재 스키마가 VARCHAR이고 기존 코드에 문자·숫자 상태값이 공존해 길이만 제한한다.
    @Size(max = 20, message = "계모임 상태는 20자 이내로 입력해 주세요.")
    private String status;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 10;

    private int offset;
    private int limit;

    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
        // AI작성: 관리자 검색도 %, _, !를 문자 그대로 찾도록 일반 목록의 LIKE 규칙과 맞춘다.
        this.keywordPattern = this.keyword == null ? null
                : "%" + this.keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    // AI작성: DB에 지역 코드가 없으므로 기존 계모임 목록과 같은 순서로 지역명으로 변환한다.
    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
        if (regionCode != null && regionCode.matches("0[1-9]|10")) {
            this.regionName = REGIONS.get(Integer.parseInt(regionCode) - 1);
        } else {
            this.regionName = null;
        }
    }
}
