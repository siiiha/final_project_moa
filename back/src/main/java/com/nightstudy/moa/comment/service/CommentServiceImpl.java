package com.nightstudy.moa.comment.service;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.mapper.CommentMapper;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {
    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    @Autowired
    public CommentServiceImpl(
            CommentMapper commentMapper,
            PostMapper postMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
    }

    @Override
    public CommentDto insertComment(Long postId, Long memberId, String content) {
        if(postMapper.countPostById(postId) == 0){
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }

        if (content == null || content.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "COMMENT_CONTENT_REQUIRED",
                    "댓글 내용을 입력해주세요."
            );
        }

        CommentDto comment = new CommentDto();
        comment.setPostId(postId);
        comment.setMemberId(memberId);
        comment.setContent(content);

        int insertRows = commentMapper.insertComment(comment);

        if (insertRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "COMMENT_CREATE_FAILED",
                    "댓글 등록에 실패했습니다."
            );
        }

        return commentMapper.selectCommentById(comment.getCommentId());
    }
}
