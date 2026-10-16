package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import java.util.List;


public interface CommentService {
    CommentDto insertComment(Long postId, Long memberId, String content);

}
