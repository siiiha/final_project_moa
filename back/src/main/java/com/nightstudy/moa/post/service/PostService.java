package com.nightstudy.moa.post.service;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListDto;
import com.nightstudy.moa.post.dto.PostSearchDto;


public interface PostService {
    Long writePost(PostDto postDto);

    PostListDto getPostList(PostSearchDto postSearchDto);

    int selectPostListCount(PostListDto postListDto);

}