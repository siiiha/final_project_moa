package com.nightstudy.moa.post.controller;

import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListDto;
import com.nightstudy.moa.post.dto.PostSearchDto;
import com.nightstudy.moa.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
    public String write(@ModelAttribute PostDto postDto) {
        Long postId = postService.writePost(postDto);
        return "redirect:/community/detail/" + postId;
    }

    @GetMapping("/list")
    public String list(@ModelAttribute Model model, PostSearchDto searchDto) {
        PostListDto result = postService.getPostList(searchDto);
        model.addAttribute("postList", result.getPostList());
        model.addAttribute("pageInfo", result.getpageInfo());
        
        return "board/list";
    }


}