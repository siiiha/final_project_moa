package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.service.AdminPostService;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.post.dto.PostSearchCondition;
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
// AI작성: 관리자 게시글 Controller의 요청값 오류와 공통 오류 응답 형식을 검증한다.
class AdminPostControllerTest {

    @Mock
    private AdminPostService adminPostService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminPostController(adminPostService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    // AI작성: 숫자가 아닌 페이지 요청값은 Controller 실행 전에 400 공통 응답으로 처리되는지 확인한다.
    void getPostListReturnsBadRequestForInvalidPageType() throws Exception {
        mockMvc.perform(get("/api/admin/posts").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    // AI작성: 게시글 목록 DB 오류는 상세 원인을 숨기고 500 공통 응답으로 반환하는지 확인한다.
    void getPostListReturnsInternalErrorWhenDatabaseFails() throws Exception {
        when(adminPostService.getPostList(any(PostSearchCondition.class)))
                .thenThrow(new DataAccessResourceFailureException("테스트용 DB 연결 오류"));

        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    // AI작성: 존재하지 않는 게시글 상세 요청은 Service 예외의 404 코드와 메시지를 유지하는지 확인한다.
    void getPostDetailReturnsNotFound() throws Exception {
        when(adminPostService.getPostDetail(999L)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "POST_NOT_FOUND",
                "게시글을 찾을 수 없습니다."
        ));

        mockMvc.perform(get("/api/admin/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
    }
}
