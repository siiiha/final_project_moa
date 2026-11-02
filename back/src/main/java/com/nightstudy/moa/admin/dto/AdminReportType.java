package com.nightstudy.moa.admin.dto;

// AI작성: 관리자 신고 검색에서 허용하는 신고 대상 유형을 DB의 report_type 값과 동일하게 관리한다.
public enum AdminReportType {
    POST,
    COMMENT,
    MEMBER,
    GROUP,
    GROUP_MEMBER,
    CHAT_MESSAGE
}
