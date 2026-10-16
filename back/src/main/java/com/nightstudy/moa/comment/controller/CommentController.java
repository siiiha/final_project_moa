package com.nightstudy.moa.comment.controller;

import com.nightstudy.moa.comment.dto.CommentDto;
import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/post/{postId}/comment")
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
}
