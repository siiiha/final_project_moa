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

        if (categoryCount != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "존재하지 않는 카테고리입니다."
            );
        }

        int insertedRows = postMapper.insertPost(postDto);
        if (insertedRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "POST_WRITE_FAILED",
                    "게시글 등록에 실패했습니다."
            );
        }
        
        return postDto.getPostId();
    }
}