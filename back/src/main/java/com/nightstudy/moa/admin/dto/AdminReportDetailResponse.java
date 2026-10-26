package com.nightstudy.moa.admin.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
// AI작성: 관리자 신고 상세 화면에 사건 정보와 개별 신고 목록을 함께 전달하는 응답 DTO이다.
public class AdminReportDetailResponse {
    // AI작성: 신고 유형, 대상 회원, 처리 상태 등 신고 사건의 기본 정보이다.
    private final AdminReportListItemResponse reportCase;
    // AI작성: 같은 사건에 접수된 신고자별 상세 신고 내용이다.
    private final List<AdminReportSubmissionResponse> submissions;
}
