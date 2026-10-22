package com.nightstudy.moa.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 댓글 소프트 삭제 SQL을 연결한다.
public interface AdminCommentMapper {
    int deleteComment(
            @Param("postId") Long postId,
            @Param("commentId") Long commentId
    );
}
