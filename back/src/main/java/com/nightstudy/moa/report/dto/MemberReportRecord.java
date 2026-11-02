package com.nightstudy.moa.report.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// AI작성: 개별 회원 신고 저장값과 MyBatis가 반환하는 생성 번호를 함께 보관한다.
public class MemberReportRecord {
    private Long reportId;
    private final Long caseId;
    private final Long reporterId;
    private final String reason;

    public MemberReportRecord(Long caseId, Long reporterId, String reason) {
        this.caseId = caseId;
        this.reporterId = reporterId;
        this.reason = reason;
    }
}
