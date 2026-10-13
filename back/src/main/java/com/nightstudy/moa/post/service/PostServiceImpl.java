package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.mapper.PostMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;

    public PostServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public Long writePost(PostDto postDto) {
        int categoryCount =
                postMapper.countCategoryById(postDto.getCategoryId());

        if (categoryCount == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "존재하지 않는 카테고리입니다."
            );
        }

        postMapper.insertPost(postDto);
        return postDto.getPostId();
    }
}