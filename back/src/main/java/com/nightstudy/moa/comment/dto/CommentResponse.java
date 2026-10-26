package com.nightstudy.moa.comment.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentResponse {

    private Long commentId;
    private Long postId;
    private Long parentId;
    private Long memberId;
    private String writerNickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
    // AI작성: 원댓글 아래에 한 단계 대댓글만 담아 반환합니다.
    private List<CommentResponse> replies = new ArrayList<>();
}
