package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;


public interface CommentService {
    CommentDto insertComment(Long postId, Long memberId, String content);

    CommentListResult getCommentList(
            Long postId,
            CommentSearchCondition condition);

}
