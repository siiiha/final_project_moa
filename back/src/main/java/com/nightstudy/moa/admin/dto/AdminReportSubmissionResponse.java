package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
// AI작성: 신고 사건에 접수된 개별 신고자의 내용과 증빙 정보를 표시하는 응답 DTO이다.
public class AdminReportSubmissionResponse {

    // AI작성: 개별 신고 식별자와 신고자 기본 정보이다.
    private Long reportId;
    private Long reporterId;
    private String reporterNickname;
    private String reporterEmail;

    // AI작성: 사용자가 작성한 신고 사유와 신고 접수 시각이다.
    private String reason;
    private LocalDateTime reportedAt;

    // AI작성: 신고 증빙 파일 정보와 증빙 원본 삭제 완료 시각이다.
    private String evidenceFiles;
    private LocalDateTime evidenceDeletedAt;
}
