package com.nightstudy.moa.admin.dto;

// AI작성: 관리자가 조회하고 변경하는 소모임 운영 상태를 안전한 타입으로 관리한다.
public enum AdminSmallGroupStatus {
    ACTIVE,
    RESTRICTED;

    public static AdminSmallGroupStatus from(String value) {
        try {
            return AdminSmallGroupStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("지원하지 않는 소모임 상태입니다: " + value, exception);
        }
    }
}
