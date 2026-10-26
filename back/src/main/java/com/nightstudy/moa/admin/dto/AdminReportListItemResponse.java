package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 신고 목록과 상세의 기본 영역에 신고 사건 한 건을 표시하는 응답 DTO이다.
public class AdminReportListItemResponse {

    private Long caseId;

    // BOARD_POST, BOARD_COMMENT, BOARD_MEMBER, MEETING, MEETING_MEMBER
    private String reportType;

    // 게시글 번호, 댓글 번호, 모임 번호 또는 회원 번호
    private Long targetId;

    private Long reportedMemberId;

    private String reportedMemberNickname;

    private int reporterCount;

    // PENDING, CONFIRMED, REJECTED
    private String status;

    // AI작성: 관리자가 작성한 위반 확정 또는 반려 사유이다.
    private String processReason;

    // AI작성: 신고 사건이 최초 생성된 일시이다.
    private LocalDateTime createdAt;

    // AI작성: 관리자가 신고 사건을 최종 처리한 일시이다.
    private LocalDateTime processedAt;

    // AI작성: 이 사건으로 발생한 기능 제한 종료 일시이다.
    // 기능 제한이 적용되지 않았다면 null
    private LocalDateTime restrictedUntil;
}
