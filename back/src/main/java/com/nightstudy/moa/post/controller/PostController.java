package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostCategoryDto;
import com.nightstudy.moa.post.dto.PostCreateRequest;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;




@RestController
@RequestMapping("/api/community")
public class PostController {
    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/write")
    public ApiResponse<Long> write(
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.writePost(loginMemberId, request);
        return ApiResponse.ok(postId);
    }

    @GetMapping("/list")
    public ApiResponse<List<PostDto>> list(
            @ModelAttribute PostSearchCondition condition
    ) {
        PostListResult result = postService.getPostList(condition);

        return ApiResponse.ok(
                result.getPostList(),
                result.getPageMeta()
        );
    }

    @GetMapping("/categories")
    public ApiResponse<List<PostCategoryDto>> categories() {
        return ApiResponse.ok(postService.getPostCategories());
    }

    @GetMapping("/detail/{postId}")
    public ApiResponse<PostDto> detail(
            @PathVariable Long postId,
            @RequestAttribute("memberId") Long loginMemberId
    ) {
        PostDto post = postService.getPostDetail(postId, loginMemberId);
        return ApiResponse.ok(post);
    }

    @PutMapping("/edit/{postId}")
    public ApiResponse<Long> editPost(
            @PathVariable Long postId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody PostEditRequest request) {
        Long editPostId = postService.editPost(postId, loginMemberId, request);

        return ApiResponse.ok(editPostId);
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable Long postId,
            @RequestAttribute("memberId") Long loginMemberId) {
        postService.deletePost(postId, loginMemberId);

        return ApiResponse.ok(null);
    }
}
