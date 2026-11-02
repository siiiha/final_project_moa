package com.nightstudy.moa.admin.dto;

import java.util.Arrays;
import lombok.Getter;

@Getter
// AI작성: 직접 제한과 신고 기반 제한이 동일한 API 값과 제한 일수를 사용한다.
public enum MemberRestrictionType {
    SEVEN_DAYS("7_DAYS", 7),
    THIRTY_DAYS("30_DAYS", 30),
    PERMANENT("PERMANENT", null);

    private final String value;
    private final Integer days;

    // AI작성: 외부 API 값과 실제 제한 일수를 하나의 제한 유형으로 묶는다.
    MemberRestrictionType(String value, Integer days) {
        this.value = value;
        this.days = days;
    }

    // AI작성: 검증된 API 문자열을 기간 계산에 사용할 enum으로 변환한다.
    public static MemberRestrictionType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 이용 제한 유형입니다."));
    }

    // AI작성: DB와 API에서 사용하는 기존 제한 유형 문자열을 반환한다.
    public String getValue() {
        return value;
    }

    // AI작성: 종료 시각이 없는 영구 제한인지 문자열 비교 없이 확인한다.
    public boolean isPermanent() {
        return this == PERMANENT;
    }
}
