package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.service.AdminCommentService;
import com.nightstudy.moa.comment.dto.CommentListResult;
import com.nightstudy.moa.comment.dto.CommentResponse;
import com.nightstudy.moa.comment.dto.CommentSearchCondition;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 댓글 API의 정상 응답과 요청·DB·삭제 예외 응답을 검증한다.
class AdminCommentControllerTest {

    @Mock
    private AdminCommentService adminCommentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminCommentController(adminCommentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    // AI작성: 관리자 댓글 목록이 전체 댓글 수와 페이징 정보를 포함해 반환되는지 확인한다.
    void getCommentListReturnsCommentsAndPageMeta() throws Exception {
        CommentResponse comment = new CommentResponse();
        comment.setCommentId(10L);
        comment.setContent("댓글");
        CommentListResult result = new CommentListResult(
                List.of(comment),
                PageMeta.of(1, 20, 1),
                2
        );
        when(adminCommentService.getCommentList(
                org.mockito.ArgumentMatchers.eq(1L),
                any(CommentSearchCondition.class)
        )).thenReturn(result);

        mockMvc.perform(get("/api/admin/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comments[0].commentId").value(10))
                .andExpect(jsonPath("$.data.totalCommentCount").value(2))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    // AI작성: 숫자가 아닌 댓글 페이지 값은 400 공통 응답으로 처리되는지 확인한다.
    void getCommentListReturnsBadRequestForInvalidPageType() throws Exception {
        mockMvc.perform(get("/api/admin/posts/1/comments").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    // AI작성: 관리자 댓글 삭제 대상이 없으면 404 공통 응답으로 반환되는지 확인한다.
    void deleteCommentReturnsNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "COMMENT_NOT_FOUND",
                "댓글을 찾을 수 없습니다."
        )).when(adminCommentService).deleteComment(1L, 999L);

        mockMvc.perform(delete("/api/admin/posts/1/comments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    // AI작성: 댓글 조회 중 DB 오류는 내부 정보를 숨긴 500 공통 응답으로 반환되는지 확인한다.
    void getCommentListReturnsInternalErrorWhenDatabaseFails() throws Exception {
        when(adminCommentService.getCommentList(
                org.mockito.ArgumentMatchers.eq(1L),
                any(CommentSearchCondition.class)
        )).thenThrow(new DataAccessResourceFailureException("테스트용 DB 연결 오류"));

        mockMvc.perform(get("/api/admin/posts/1/comments"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
    }
}
