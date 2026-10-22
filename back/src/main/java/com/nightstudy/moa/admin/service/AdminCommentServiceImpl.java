package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.mapper.AdminCommentMapper;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// AI작성: 기존 댓글 조회 규칙을 재사용하고 관리자 전용 댓글 삭제를 처리한다.
public class AdminCommentServiceImpl implements AdminCommentService {
    private final CommentService commentService;
    private final AdminCommentMapper adminCommentMapper;

    @Autowired
    public AdminCommentServiceImpl(
            CommentService commentService,
            AdminCommentMapper adminCommentMapper
    ) {
        this.commentService = commentService;
        this.adminCommentMapper = adminCommentMapper;
    }

    @Override
    public CommentListResult getCommentList(
            Long postId,
            CommentSearchCondition condition
    ) {
        // AI작성: 대댓글 조립과 전체 댓글 수 계산이 중복되지 않도록 기존 조회 기능을 재사용한다.
        return commentService.getCommentList(postId, condition);
    }

    @Override
    public void deleteComment(Long postId, Long commentId) {
        // AI작성: 이미 삭제됐거나 해당 게시글에 속하지 않은 댓글은 찾을 수 없음으로 처리한다.
        if (adminCommentMapper.deleteComment(postId, commentId) != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "COMMENT_NOT_FOUND",
                    "댓글을 찾을 수 없습니다."
            );
        }
    }
}
