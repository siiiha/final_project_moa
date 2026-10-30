package com.nightstudy.moa.comment.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.comment.service.CommentService;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

@ExtendWith(MockitoExtension.class)
// AI작성: 실제 @Validated 프록시를 적용해 댓글 API 경로 번호 검증과 공통 400 응답을 확인합니다.
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MethodValidationPostProcessor validationProcessor = new MethodValidationPostProcessor();
        validationProcessor.setProxyTargetClass(true);
        validationProcessor.afterPropertiesSet();

        Object validatedController = validationProcessor.postProcessAfterInitialization(
                new CommentController(commentService),
                "commentController"
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(validatedController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getCommentListRejectsZeroPostId() throws Exception {
        mockMvc.perform(get("/api/post/0/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }

    @Test
    void insertCommentRejectsNegativePostId() throws Exception {
        mockMvc.perform(post("/api/post/-1/comments")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }

    @Test
    void insertReplyRejectsZeroParentCommentId() throws Exception {
        mockMvc.perform(post("/api/post/1/comments/0/replies")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"대댓글\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }

    @Test
    void updateCommentRejectsNegativeCommentId() throws Exception {
        mockMvc.perform(put("/api/post/1/comments/-1")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정 댓글\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }

    @Test
    void deleteCommentRejectsZeroCommentId() throws Exception {
        mockMvc.perform(delete("/api/post/1/comments/0")
                        .requestAttr("memberId", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verifyNoInteractions(commentService);
    }
}
