package com.nightstudy.moa.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 소모임 목록의 활동 지역을 시·도/시·군·구/읍·면·동 순서로 필터링한다.
public class AdminSmallGroupSearchCondition {
    @Size(max = 20, message = "검색어는 20자 이내로 입력해 주세요.")
    private String keyword;

    @Size(max = 30, message = "시·도 지역명을 다시 선택해 주세요.")
    private String provinceName;

    @Size(max = 50, message = "시·군·구 지역명을 다시 선택해 주세요.")
    private String districtName;

    @Size(max = 50, message = "읍·면·동 지역명을 다시 선택해 주세요.")
    private String townName;

    // AI작성: 관리자 소모임 목록에서 화면이 지원하는 활동·제한 상태만 검색 조건으로 허용한다.
    @Pattern(regexp = "ACTIVE|RESTRICTED", message = "소모임 상태를 다시 선택해 주세요.")
    private String status;

    @NotNull(message = "페이지 번호를 올바르게 입력해 주세요.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    private String keywordPattern;
    private int offset;
    private int limit;

    public void setKeyword(String keyword) { this.keyword = normalize(keyword); }
    public void setProvinceName(String provinceName) { this.provinceName = normalize(provinceName); }
    public void setDistrictName(String districtName) { this.districtName = normalize(districtName); }
    public void setTownName(String townName) { this.townName = normalize(townName); }

    private String normalize(String value) {
        return value == null || value.strip().isEmpty() ? null : value.strip();
    }
}
