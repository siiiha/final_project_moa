package com.nightstudy.moa.post.service;

import com.nightstudy.moa.post.dto.PostDto;

public interface PostService {
    Long writePost(PostDto postDto);
}