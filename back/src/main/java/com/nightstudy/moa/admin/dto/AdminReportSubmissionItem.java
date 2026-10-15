package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;

//report_case 한 건에 연결된 여러 member_report
public class AdminReportSubmissionItem {

    private Long reportId;
    private Long reporterId;
    private String reporterNickname;
    private String reporterEmail;
    private String reason;
    private LocalDateTime reportedAt;
    private String evidenceFiles;
    private LocalDateTime evidenceDeletedAt;
}
