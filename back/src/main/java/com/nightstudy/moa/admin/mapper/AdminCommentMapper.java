package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.admin.dto.AdminCommentListItemResponse;
import com.nightstudy.moa.admin.dto.AdminCommentSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 전체 댓글 목록 조회와 댓글 소프트 삭제 SQL을 연결한다.
public interface AdminCommentMapper {
    int selectAdminCommentCount(AdminCommentSearchCondition condition);

    List<AdminCommentListItemResponse> selectAdminCommentList(
            AdminCommentSearchCondition condition
    );

    int deleteComment(
            @Param("postId") Long postId,
            @Param("commentId") Long commentId
    );
}
