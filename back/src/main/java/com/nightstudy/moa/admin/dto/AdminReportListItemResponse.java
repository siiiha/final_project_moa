package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
//신고 목록 중에서 신고 한 건을 표시하는 DTO
public class AdminReportListItemResponse {

    // 신고 사건 고유 번호
    private Long caseId;
    // 신고 유형
    // BOARD_POST, BOARD_COMMENT, BOARD_MEMBER, MEETING, MEETING_MEMBER
    private String reportType;

    // 신고 대상의 실제 PK
    // 게시글 번호, 댓글 번호, 모임 번호 또는 회원 번호
    private Long targetId;

    // 위반 확정 시 제재 대상이 되는 회원 번호
    private Long reportedMemberId;

    // 관리자 화면에 표시할 제재 대상 회원 닉네임
    private String reportedMemberNickname;

    // 같은 신고 사건에 접수된 서로 다른 신고자 수
    private int reporterCount;

    // 신고 사건 처리 상태
    // PENDING, CONFIRMED, REJECTED
    private String status;

    // 관리자가 작성한 위반 확정 또는 반려 사유
    private String processReason;

    // 신고 사건이 최초 생성된 일시
    private LocalDateTime createdAt;

    // 관리자가 신고 사건을 최종 처리한 일시
    private LocalDateTime processedAt;

    // 이 사건으로 발생한 기능 제한 종료 일시
    // 기능 제한이 적용되지 않았다면 null
    private LocalDateTime restrictedUntil;
}
