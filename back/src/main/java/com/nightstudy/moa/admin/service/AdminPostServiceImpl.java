package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.mapper.AdminPostMapper;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
// AI작성: 관리자 게시글 조회와 게시글·댓글 소프트 삭제 규칙을 처리한다.
public class AdminPostServiceImpl implements AdminPostService {
    private final AdminPostMapper adminPostMapper;

    @Autowired
    public AdminPostServiceImpl(AdminPostMapper adminPostMapper) {
        this.adminPostMapper = adminPostMapper;
    }

    @Override
    public PostListResult getPostList(PostSearchCondition condition) {
        int total = adminPostMapper.selectPostCount(condition);
        PageMeta pageMeta = PageMeta.of(
                condition.getPage(),
                condition.getSize(),
                total
        );

        if (total == 0) {
            return new PostListResult(List.of(), pageMeta);
        }

        // AI작성: PageMeta가 정규화한 값을 목록 SQL의 LIMIT 조건으로 전달한다.
        condition.setOffset(pageMeta.getOffset());
        condition.setLimit(pageMeta.getSize());

        return new PostListResult(
                adminPostMapper.selectPostList(condition),
                pageMeta
        );
    }

    @Override
    public PostDto getPostDetail(Long postId) {
        PostDto post = adminPostMapper.selectPostDetail(postId);

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
    public void deletePost(Long postId) {
        // AI작성: 관리자 권한은 URL 보안 설정에서 검사하므로 작성자 조건 없이 삭제한다.
        if (adminPostMapper.deletePost(postId) != 1) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다."
            );
        }
    }

}
