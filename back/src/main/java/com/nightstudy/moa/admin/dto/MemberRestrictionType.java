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

    MemberRestrictionType(String value, Integer days) {
        this.value = value;
        this.days = days;
    }

    public static MemberRestrictionType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("잘못된 이용 제한 유형입니다."));
    }

    public String getValue() {
        return value;
    }

    public boolean isPermanent() {
        return this == PERMANENT;
    }
}
