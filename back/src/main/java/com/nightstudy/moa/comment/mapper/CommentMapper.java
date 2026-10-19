package com.nightstudy.moa.comment.mapper;

import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.nightstudy.moa.comment.dto.CommentDto;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.filter.CommonsRequestLoggingFilter;


@Mapper
public interface CommentMapper {
    int insertComment(CommentDto commentDto);
    CommentDto selectCommentById(Long commentId);

    int selectCommentCountByPostId(Long postId);

    List<CommentResponse> selectCommentListByPostId(
            CommentSearchCondition condition
    );

    int updateComment(
            @Param("postId") Long postId,
            @Param("commentId") Long commentId,
            @Param("request") CommentEditRequest request
    );

}
