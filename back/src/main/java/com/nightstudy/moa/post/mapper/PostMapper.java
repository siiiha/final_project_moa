package com.nightstudy.moa.post.mapper;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper {
    int insertPost(PostDto postDto);

    List<PostDto> selectPostList(PostSearchCondition condition);

    PostDto selectPostDetail(Long postId);

    int selectPostListCount(PostSearchCondition condition);

    Long editPost(
            @Param("postId") Long postId,
            @Param("request") PostEditRequest request
    );

    int countCategoryById(
            @Param("categoryId") Long categoryId
    );
    int increaseViewCount(
            @Param("postId") Long postId,
            @Param("loginMemberId") Long loginMemberId
    );

    int deletePost(
            @Param("postId") Long postId,
            @Param("writerId") Long writerId
    );
}

