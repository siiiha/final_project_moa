package com.nightstudy.moa.report.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// AI작성: 신고 사건 저장값과 MyBatis가 반환하는 생성 번호를 함께 보관한다.
public class ReportCaseRecord {
    private Long caseId;
    private final ReportType reportType;
    private final Long targetId;
    private final Long reportedMemberId;

    public ReportCaseRecord(ReportType reportType, Long targetId, Long reportedMemberId) {
        this.reportType = reportType;
        this.targetId = targetId;
        this.reportedMemberId = reportedMemberId;
    }
}
