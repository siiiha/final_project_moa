package com.nightstudy.moa.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 신고 상세에서 게시글·댓글 원문과 삭제 상태를 확인하기 위한 응답 DTO입니다.
public class AdminReportTargetResponse {
    private String reportType;
    private Long targetId;
    private Long postId;
    private String postTitle;
    private String content;
    private Long writerId;
    private String writerNickname;
    private boolean deleted;
}
