package com.nightstudy.moa.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// 지역 인증에 사용할 현재 좌표와 예상 오차를 전달받음
@Getter
@Setter
public class RegionVerificationRequest {

    @NotNull(message = "현재 위치의 위도가 필요합니다.")
    @DecimalMin(value = "-90.0", message = "위도 값이 올바르지 않습니다.")
    @DecimalMax(value = "90.0", message = "위도 값이 올바르지 않습니다.")
    private Double latitude;

    @NotNull(message = "현재 위치의 경도가 필요합니다.")
    @DecimalMin(value = "-180.0", message = "경도 값이 올바르지 않습니다.")
    @DecimalMax(value = "180.0", message = "경도 값이 올바르지 않습니다.")
    private Double longitude;

    @NotNull(message = "위치 정확도 정보가 필요합니다.")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "위치 정확도 값이 올바르지 않습니다."
    )
    @DecimalMax(
            value = "500.0",
            message = "위치가 정확하지 않습니다. 예상 오차 반경 500m 이하에서 다시 시도해주세요."
    )
    private Double accuracy;
}