package com.nightstudy.moa.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 삭제되지 않은 신고 대상과 실제 작성자 또는 대상 회원 번호를 전달한다.
public class ReportTarget {
    private Long reportedMemberId;
}
