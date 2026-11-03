package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
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

    // AI작성: 선택한 단계까지만 조회하며 기존 지역 코드와 함께 사용할 수 없다.
    @Size(max = 30, message = "시·도 지역명을 다시 선택해 주세요.")
    private String provinceName;
    @Size(max = 50, message = "시·군·구 지역명을 다시 선택해 주세요.")
    private String districtName;
    @Size(max = 50, message = "읍·면·동 지역명을 다시 선택해 주세요.")
    private String townName;

    public void setProvinceName(String value) { this.provinceName = normalize(value); }
    // AI작성: 세종의 구 이름은 빈 문자열이므로 미전달(null)과 구분한다.
    public void setDistrictName(String value) { this.districtName = value == null ? null : value.strip(); }
    public void setTownName(String value) { this.townName = normalize(value); }

    @AssertTrue(message = "상위 지역을 선택하고 기존 지역 코드와 새 지역 조건 중 하나만 사용해 주세요.")
    public boolean isRegionSelectionValid() {
        boolean hasArea = provinceName != null || districtName != null || townName != null;
        return !(regionCode != null && hasArea)
                && (districtName == null || provinceName != null)
                && (townName == null || (provinceName != null && districtName != null));
    }

    private String normalize(String value) {
        return value == null || value.strip().isEmpty() ? null : value.strip();
    }

    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    // AI작성: Validation보다 먼저 앞뒤 공백을 제거하고 공백 검색을 해제한다.
    public void setKeyword(String keyword) {
        this.keyword = keyword == null || keyword.strip().isEmpty() ? null : keyword.strip();
    }
}
