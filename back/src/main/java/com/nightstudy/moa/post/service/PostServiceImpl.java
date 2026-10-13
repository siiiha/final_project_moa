package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.mapper.PostMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService {
    private final PostMapper postMapper;

    @Autowired
    public PostServiceImpl(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    public Long writePost(PostDto postDto) {
        postMapper.insertPost(postDto);

        return postDto.getPostId();
    }

    @Override
    public PostListResult getPostList(PostSearchCondition condition) {

        int totalCount = postMapper.selectPostListCount(condition);
        PageMeta pageMeta = PageMeta.of(condition.getPage(), condition.getSize(), totalCount);

        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        List<PostDto> postList = postMapper.selectPostList(condition);
        return new PostListResult(postList, pageMeta);
    }

    @Override
//    게시글 번호를 받아서 → 조회수를 증가시키고 → 게시글 정보를 조회하고 → 이미지까지 추가한 다음 → 완성된 게시글 정보를 반환하는 코드
    public PostDto getPostDetail(Long postId) {
        PostDto post = postMapper.selectPostDetail(postId);

        if (post == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
        return post;
    }


    @Override
    public Long editPost(Long postId, PostEditRequest request) {

        int categoryCount =
                postMapper.countCategoryById(request.getCategoryId());

        if (categoryCount != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CATEGORY",
                    "존재하지 않는 카테고리입니다."
            );
        }

        Long editCount = postMapper.editPost(postId, request);

        if (editCount != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
        return postId;
    }


}