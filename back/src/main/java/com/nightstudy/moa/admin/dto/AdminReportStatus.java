package com.nightstudy.moa.admin.dto;

// AI작성: 관리자 신고 검색에서 허용하는 처리 상태를 DB의 status 값과 동일하게 관리한다.
public enum AdminReportStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}
