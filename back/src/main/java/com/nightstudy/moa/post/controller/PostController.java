package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
            @Valid @ModelAttribute PostDto postDto
    ) {
        Long postId = postService.writePost(postDto);
        return ApiResponse.ok(postId);
    }
}