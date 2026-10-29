package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.AdminCommentListResult;
import com.nightstudy.moa.admin.dto.AdminCommentSearchCondition;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;

// AI작성: 관리자 댓글 조회와 삭제 기능의 사용 규약을 정의한다.
public interface AdminCommentService {
    AdminCommentListResult getAdminCommentList(
            AdminCommentSearchCondition condition
    );

    CommentListResult getCommentList(
            Long postId,
            CommentSearchCondition condition
    );

    void deleteComment(Long postId, Long commentId);
}
