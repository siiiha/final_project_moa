package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;


public interface CommentService {
    CommentDto insertComment(Long postId, Long memberId, String content);

    // AI작성: 부모 댓글 검증을 포함한 대댓글 등록 기능입니다.
    CommentDto insertReply(Long postId, Long parentCommentId, Long memberId, String content);

    CommentListResult getCommentList(
            Long postId,
            CommentSearchCondition condition);

    CommentDto updateComment(Long postId, Long commentId, Long loginMemberId, CommentEditRequest request);

    void deleteComment(Long postId, Long commentId, Long loginMemberId);

}
