package com.nightstudy.moa.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.nightstudy.moa.comment.dto.CommentDto;


@Mapper
public interface CommentMapper {
    int insertComment(CommentDto commentDto);
    CommentDto selectCommentById(Long commentId);

}
