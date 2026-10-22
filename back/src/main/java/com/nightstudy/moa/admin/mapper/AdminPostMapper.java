package com.nightstudy.moa.admin.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// AI작성: 관리자 게시글 조회와 게시글·댓글 소프트 삭제 SQL을 연결한다.
public interface AdminPostMapper {
    List<PostDto> selectPostList(PostSearchCondition condition);

    int selectPostCount(PostSearchCondition condition);

    PostDto selectPostDetail(@Param("postId") Long postId);

    int deletePost(@Param("postId") Long postId);
}
