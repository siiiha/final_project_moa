package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import com.nightstudy.moa.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/community")
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
    public PostListResult list(
            @ModelAttribute PostSearchCondition condition
    ) {
        return postService.getPostList(condition);
    }

    @GetMapping("/detail/{postId}")
    public PostDto detail(@PathVariable Long postId) {
        return postService.getPostDetail(postId);
    }


}