package com.nightstudy.moa.comment.controller;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentCreateRequest;
import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
public class CommentController {
    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) { this.commentService = commentService;
    }

    @PostMapping("/post/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> insertComment(
            @PathVariable Long postId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentDto insertedComment = commentService.insertComment(postId,
                loginMemberId,
                request.getContent());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(insertedComment));
    }

    // AI작성: 원댓글 번호를 경로로 받아 한 단계 대댓글을 등록합니다.
    @PostMapping("/post/{postId}/comments/{parentCommentId}/replies")
    public ResponseEntity<ApiResponse<CommentDto>> insertReply(
            @PathVariable Long postId,
            @PathVariable Long parentCommentId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentDto insertedReply = commentService.insertReply(
                postId,
                parentCommentId,
                loginMemberId,
                request.getContent());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(insertedReply));
    }

    @GetMapping("/post/{postId}/comments")
    public ApiResponse<CommentListResult> getCommentList(
            @PathVariable Long postId,
            @ModelAttribute CommentSearchCondition condition
            ) {
        // AI작성: 게시글 번호와 페이지 조건으로 댓글 목록을 조회합니다.
        CommentListResult result = commentService.getCommentList(postId, condition);

        // AI작성: 원댓글 페이징 정보와 대댓글을 포함한 전체 댓글 개수를 함께 반환합니다.
        return ApiResponse.ok(result, result.getPageMeta());
    }

    @PutMapping("/post/{postId}/comments/{commentId}")
    public ApiResponse<CommentDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestAttribute("memberId") Long loginMemberId,
            @Valid @RequestBody CommentEditRequest request) {
        CommentDto updateComment = commentService.updateComment(postId,
                commentId, loginMemberId, request);

        return  ApiResponse.ok(updateComment);
        }

        @DeleteMapping("/post/{postId}/comments/{commentId}")
        public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestAttribute("memberId") Long loginMemberId) {

        commentService.deleteComment(postId, commentId, loginMemberId);

        return ApiResponse.ok(null);
        }


}
