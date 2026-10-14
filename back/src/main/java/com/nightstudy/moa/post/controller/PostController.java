package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostEditRequest;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nightstudy.moa.post.service.PostService;
import jakarta.validation.Valid;



@RestController
@RequestMapping("/api/community")
public class PostController {
    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/write")
    public Long write(@RequestBody PostDto postDto) {
        return postService.writePost(postDto);
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

    @GetMapping("/detail/{postId}")
    public ApiResponse<PostDto> detail(
            @PathVariable Long postId
    ) {
        // TODO: 토큰 인증 구현 후 로그인 회원 ID를 전달하도록 변경
        PostDto post = postService.getPostDetail(postId, null);
        return ApiResponse.ok(post);
    }

    @PutMapping("/edit/{postId}")
    public ApiResponse<Long> editPost(
            @PathVariable Long postId,
            @Valid @RequestBody PostEditRequest request) {
        Long editPostId = postService.editPost(postId, request);

        return ApiResponse.ok(editPostId);
    public ApiResponse<Long> write(
            @Valid @ModelAttribute PostDto postDto
    ) {
        Long postId = postService.writePost(postDto);
        return ApiResponse.ok(postId);
    }
}