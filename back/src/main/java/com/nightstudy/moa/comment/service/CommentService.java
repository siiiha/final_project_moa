package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;


public interface CommentService {
    CommentDto insertComment(Long postId, Long memberId, String content);

}
