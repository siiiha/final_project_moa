package com.nightstudy.moa.report.dto;

// AI작성: 신고 유형 문자열의 중복과 불일치를 방지하기 위해 게시글·댓글·회원 유형을 공통 Enum으로 관리한다.
// AI작성: ReportMapper.xml에서도 문자열이 아닌 이 Enum 값을 직접 비교한다.
public enum ReportType {
    POST,
    COMMENT,
    MEMBER,
    // AI작성: 계모임과 소모임은 공통 groups.group_id로 식별하므로 하나의 신고 유형을 재사용한다.
    GROUP
}
