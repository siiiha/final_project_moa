package com.nightstudy.moa.post.service;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;


public interface PostService {
    List<PostCategoryDto> getPostCategories();
    Long writePost(Long loginMemberId, PostCreateRequest request);

    // AI작성: 이미지 첨부를 포함한 게시글 등록입니다.
    Long writePost(Long loginMemberId, PostCreateRequest request, List<MultipartFile> images);

    PostListResult getPostList(PostSearchCondition condition);

    Long editPost(Long postId, Long loginMemberId, PostEditRequest request);

    // AI작성: 글과 첨부 편집을 함께 저장합니다.
    Long editPost(Long postId, Long loginMemberId, PostEditRequest request, List<MultipartFile> images);

    PostDto getPostDetail(Long postId, Long loginMemberId);

    Resource getPostImage(Long postId, String fileName);

    void deletePost (Long postId, Long loginMemberId);
}
