package com.nightstudy.moa.comment.controller;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.dto.CommentEditRequest;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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
            @Valid @RequestBody CommentDto commentDto) {
        // TODO AI작성: Spring Security 적용 후 수정 - 요청의 memberId 대신 로그인 회원 ID를 사용해야 함
        CommentDto insertedComment = commentService.insertComment(postId,
                commentDto.getMemberId(),
                commentDto.getContent());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(insertedComment));
    }

    @GetMapping("/post/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getCommentList(
            @PathVariable Long postId,
            @ModelAttribute CommentSearchCondition condition
            ) {
        // AI작성: 게시글 번호와 페이지 조건으로 댓글 목록을 조회합니다.
        CommentListResult result = commentService.getCommentList(postId, condition);

        return ApiResponse.ok(result.getComments(), result.getPageMeta());
    }

    @PutMapping("/post/{postId}/comments/{commentId}")
    public ApiResponse<CommentDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentEditRequest request) {
        // 인증 구현 후 로그인 회원이 댓글 작성자인지 확인 필요
        CommentDto updateComment = commentService.updateComment(postId,
                commentId, request);

        return  ApiResponse.ok(updateComment);
        }

        @DeleteMapping("/post/{postId}/comments/{commentId}")
        public ApiResponse<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {

         // 인증 구현 전에는 임시 회원 ID를 사용하지 않고 댓글 번호로만 삭제
        commentService.deleteComment(postId, commentId);

        return ApiResponse.ok(null);
        }


}
