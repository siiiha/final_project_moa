package com.nightstudy.moa.post.service;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;


public interface PostService {
    Long writePost(PostDto postDto);
    PostListResult getPostList(PostSearchCondition condition);
    PostDto getPostDetail(Long postId, Long loginMemberId);
}