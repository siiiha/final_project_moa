package com.nightstudy.moa.admin.controller;

import com.nightstudy.moa.admin.dto.AdminCommentListItemResponse;
import com.nightstudy.moa.admin.dto.AdminCommentListResult;
import com.nightstudy.moa.admin.dto.AdminCommentSearchCondition;
import com.nightstudy.moa.admin.service.AdminCommentService;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin")
// AI작성: 관리자 댓글 목록 조회와 댓글 삭제 요청만 담당한다.
public class AdminCommentController {
    private final AdminCommentService adminCommentService;

    @Autowired
    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    // AI작성: 검색·지역·삭제 상태·페이지 조건으로 전체 댓글을 조회한다.
    @GetMapping("/comments")
    public ApiResponse<List<AdminCommentListItemResponse>> getAdminCommentList(
            @Valid @ModelAttribute AdminCommentSearchCondition condition
    ) {
        AdminCommentListResult result =
                adminCommentService.getAdminCommentList(condition);
        return ApiResponse.ok(result.getComments(), result.getPageMeta());
    }

    // AI작성: 기존 댓글 조회 규칙으로 원댓글·대댓글과 페이징 정보를 반환한다.
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<CommentListResult> getCommentList(
            @PathVariable Long postId,
            @ModelAttribute CommentSearchCondition condition
    ) {
        CommentListResult result =
                adminCommentService.getCommentList(postId, condition);
        return ApiResponse.ok(result, result.getPageMeta());
    }

    // AI작성: 게시글 번호와 댓글 번호가 일치하는 댓글을 관리자가 소프트 삭제한다.
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        adminCommentService.deleteComment(postId, commentId);
        return ApiResponse.ok(null);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
