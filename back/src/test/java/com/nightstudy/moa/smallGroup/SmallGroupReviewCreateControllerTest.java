package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 요청 역직렬화·Validation·인증 식별자 전달 및 공통 응답 형식을 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupReviewCreateControllerTest {
    @Mock
    private SmallGroupCreationService creationService;

    @Mock
    private SmallGroupService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SmallGroupController(creationService, service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "0.5, 30", "5, 30"})
    void returnsCreatedIdAndPassesAuthenticatedWriter(String rating, int contentLength) throws Exception {
        when(service.createSmallGroupReview(eq(10L), eq(40L), any())).thenReturn(50L);
        String content = "가".repeat(contentLength);
        // AI작성: 본문 작성자 필드를 신뢰하지 않고 서버 인증 속성의 회원 ID만 전달하는지 확인한다.
        mockMvc.perform(post("/api/small-groups/10/reviews").requestAttr("memberId", 40L)
                        .contentType("application/json")
                        .content("{\"rating\":" + rating + ",\"content\":\"" + content
                                + "\",\"writerId\":999,\"memberId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(50))
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());

        ArgumentCaptor<SmallGroupReviewCreateRequest> captor =
                ArgumentCaptor.forClass(SmallGroupReviewCreateRequest.class);
        verify(service).createSmallGroupReview(eq(10L), eq(40L), captor.capture());
        assertThat(captor.getValue().getRating()).isEqualByComparingTo(rating);
        assertThat(captor.getValue().getContent()).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"content\":\"좋아요\"}", "{\"rating\":null,\"content\":\"좋아요\"}",
            "{\"rating\":-0.5,\"content\":\"좋아요\"}",
            "{\"rating\":5.5,\"content\":\"좋아요\"}",
            "{\"rating\":0.1,\"content\":\"좋아요\"}",
            "{\"rating\":4.99,\"content\":\"좋아요\"}",
            "{\"rating\":4.5}", "{\"rating\":4.5,\"content\":null}",
            "{\"rating\":4.5,\"content\":\"\"}",
            "{\"rating\":4.5,\"content\":\"   \"}",
            "{\"rating\":4.5,\"content\":\"\\t\\n\"}",
            "{\"rating\":4.5,\"content\":\"가나다라마바가나다라마바가나다라마바가나다라마바가나다라마바가\"}"
    })
    void rejectsInvalidRequestBeforeCallingService(String body) throws Exception {
        mockMvc.perform(post("/api/small-groups/10/reviews").requestAttr("memberId", 40L)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{\"rating\":\"invalid\",\"content\":\"좋아요\"}"})
    void rejectsMissingOrUnreadableJson(String body) throws Exception {
        mockMvc.perform(post("/api/small-groups/10/reviews").requestAttr("memberId", 40L)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNonNumericPath() throws Exception {
        mockMvc.perform(post("/api/small-groups/invalid/reviews").requestAttr("memberId", 40L)
                        .contentType("application/json").content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void passesMissingAuthenticationToServiceAndPreservesUnauthorizedResponse() throws Exception {
        when(service.createSmallGroupReview(eq(10L), isNull(), any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."));
        mockMvc.perform(post("/api/small-groups/10/reviews")
                        .contentType("application/json").content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verify(service).createSmallGroupReview(eq(10L), isNull(), any());
    }

    @ParameterizedTest
    @CsvSource({
            "403, SMALL_GROUP_REVIEW_CREATE_FORBIDDEN",
            "403, SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED",
            "403, ACCESS_DENIED",
            "404, SMALL_GROUP_NOT_FOUND",
            "409, SMALL_GROUP_REVIEW_ALREADY_EXISTS",
            "409, SMALL_GROUP_REVIEW_MEET_DATE_REQUIRED",
            "409, SMALL_GROUP_REVIEW_NOT_AVAILABLE_YET",
            "500, SMALL_GROUP_REVIEW_CREATE_FAILED"
    })
    void preservesServiceErrorResponse(int httpStatus, String code) throws Exception {
        when(service.createSmallGroupReview(eq(10L), eq(40L), any()))
                .thenThrow(new ApiException(HttpStatus.valueOf(httpStatus), code, "등록할 수 없습니다."));
        mockMvc.perform(post("/api/small-groups/10/reviews").requestAttr("memberId", 40L)
                        .contentType("application/json").content(validBody()))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value("등록할 수 없습니다."));
    }

    private String validBody() {
        return "{\"rating\":4.5,\"content\":\"즐거웠어요\"}";
    }
}
