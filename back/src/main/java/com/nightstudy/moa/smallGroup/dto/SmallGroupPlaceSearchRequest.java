package com.nightstudy.moa.smallGroup.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// AI작성: 장소 검색 조건만 받으며 회원 ID는 요청값 대신 기존 JWT 인증 정보로 전달한다.
@Getter
@Setter
public class SmallGroupPlaceSearchRequest {
    @NotNull(message = "소모임을 선택해 주세요.")
    @Positive(message = "소모임 번호가 올바르지 않습니다.")
    private Long smallGroupId;

    @NotBlank(message = "장소 검색어를 입력해 주세요.")
    @Size(max = 100, message = "장소 검색어는 100자 이내로 입력해 주세요.")
    private String query;

    @NotNull(message = "페이지 번호가 필요합니다.")
    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    @Max(value = 45, message = "페이지는 45 이하여야 합니다.")
    private Integer page = 1;

    @NotNull(message = "조회 개수가 필요합니다.")
    @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
    @Max(value = 15, message = "조회 개수는 15 이하여야 합니다.")
    private Integer size = 15;

    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않습니다.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않습니다.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않습니다.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않습니다.")
    private Double longitude;

    @Min(value = 0, message = "반경은 0m 이상이어야 합니다.")
    @Max(value = 20000, message = "반경은 20,000m 이하여야 합니다.")
    private Integer radius;

    @NotNull(message = "정렬 기준이 필요합니다.")
    @Pattern(regexp = "accuracy|distance", message = "정렬 기준이 올바르지 않습니다.")
    private String sort = "accuracy";

    // AI작성: 앞뒤 공백 제거 후 길이와 필수 여부를 검증한다.
    public void setQuery(String query) {
        this.query = query == null ? null : query.strip();
    }

    @AssertTrue(message = "위도와 경도를 함께 전달해 주세요.")
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }

    // AI작성: NaN·무한대는 좌표로 허용하지 않으며 사용자 위치를 오류 문구에 포함하지 않는다.
    @AssertTrue(message = "좌표 값이 올바르지 않습니다.")
    public boolean isCoordinateFinite() {
        return (latitude == null || Double.isFinite(latitude))
                && (longitude == null || Double.isFinite(longitude));
    }

    @AssertTrue(message = "반경 검색에는 위도와 경도가 필요합니다.")
    public boolean isRadiusCoordinateValid() {
        return radius == null || (latitude != null && longitude != null);
    }

    @AssertTrue(message = "거리순 검색에는 위도와 경도가 필요합니다.")
    public boolean isDistanceSortCoordinateValid() {
        return !"distance".equals(sort) || (latitude != null && longitude != null);
    }
}
