package com.nightstudy.moa.post.service;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
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
    public PostDto getPostDetail(
            Long postId,
            Long loginMemberId
    ) {
        // 조회수 증가를 시도하고 변경된 행 개수를 받음
        int updatedRows = postMapper.increaseViewCount(postId, loginMemberId);

        // 증가된 조회수가 포함된 게시글 조회
        PostDto post = postMapper.selectPostDetail(postId);

        if (post == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }

        if (updatedRows == 1) {
            System.out.println("조회수 증가 성공");
        } else {
            System.out.println("작성자 본인이므로 조회수 증가 안 함");
        }

        return post;
    }
}