package com.nightstudy.moa.post.service;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;


public interface PostService {
    List<PostCategoryDto> getPostCategories();
    Long writePost(Long loginMemberId, PostCreateRequest request);

    PostListResult getPostList(PostSearchCondition condition);

    Long editPost(Long postId, Long loginMemberId, PostEditRequest request);

    PostDto getPostDetail(Long postId, Long loginMemberId);

    void deletePost (Long postId, Long loginMemberId);
}
