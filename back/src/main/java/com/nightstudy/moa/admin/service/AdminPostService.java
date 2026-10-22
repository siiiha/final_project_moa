package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;

// AI작성: 관리자 게시글 조회와 게시글·댓글 삭제 기능의 사용 규약을 정의한다.
public interface AdminPostService {
    PostListResult getPostList(PostSearchCondition condition);

    PostDto getPostDetail(Long postId);

    void deletePost(Long postId);

    void deleteComment(Long postId, Long commentId);
}
