package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.service.AdminPostService;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.post.dto.PostDto;
import com.nightstudy.moa.post.dto.PostListResult;
import com.nightstudy.moa.post.dto.PostSearchCondition;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/posts")
// AI작성: 관리자 게시글 목록·상세 조회와 게시글·댓글 삭제 요청을 처리한다.
public class AdminPostController {
    private final AdminPostService adminPostService;

    @Autowired
    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    // AI작성: 검색·카테고리·정렬·페이지 조건으로 관리자 게시글 목록을 조회한다.
    @GetMapping
    public ApiResponse<List<PostDto>> getPostList(
            @ModelAttribute PostSearchCondition condition
    ) {
        PostListResult result = adminPostService.getPostList(condition);
        return ApiResponse.ok(result.getPostList(), result.getPageMeta());
    }

    // AI작성: 관리 목적의 상세 조회이므로 일반 게시판과 달리 조회수를 증가시키지 않는다.
    @GetMapping("/{postId}")
    public ApiResponse<PostDto> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.ok(adminPostService.getPostDetail(postId));
    }

    // AI작성: SecurityConfig에서 ADMIN 권한을 확인한 요청의 게시글을 소프트 삭제한다.
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        adminPostService.deletePost(postId);
        return ApiResponse.ok(null);
    }

    // AI작성: 게시글과 댓글 번호를 함께 확인하여 관리 대상 댓글을 소프트 삭제한다.
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        adminPostService.deleteComment(postId, commentId);
        return ApiResponse.ok(null);
    }

    // AI작성: 빈 검색 문자열을 null로 변환하여 Mapper 검색 조건을 단순화한다.
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
