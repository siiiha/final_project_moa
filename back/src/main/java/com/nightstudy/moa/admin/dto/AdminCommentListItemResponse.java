package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 관리자 전체 댓글 목록의 한 행을 화면에 전달한다.
public class AdminCommentListItemResponse {
    private Long commentId;
    private Long postId;
    private String content;
    private String writerNickname;
    private String postTitle;
    private String regionName;
    private String status;
    private LocalDateTime createdAt;
}
