package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

// AI작성: 목록 요청 바인딩·Validation·공개 응답과 업무 오류 전달을 검증하며 권한 판단 자체는 Service 테스트에서 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupReviewListControllerTest {
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

    @Test
    void returnsAnonymousListWithDefaultPageAndPublicFields() throws Exception {
        SmallGroupReviewListItemResponse item = new SmallGroupReviewListItemResponse();
        item.setSmallGroupReviewId(50L);
        item.setWriterNickname("모아회원");
        item.setContent("즐거웠어요");
        item.setRating(new BigDecimal("4.5"));
        item.setCreatedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
        when(service.getSmallGroupReviews(eq(10L), isNull(), any()))
                .thenReturn(new SmallGroupReviewListResponse(List.of(item), PageMeta.of(1, 8, 9)));

        mockMvc.perform(get("/api/small-groups/10/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].smallGroupReviewId").value(50))
                .andExpect(jsonPath("$.data[0].writerNickname").value("모아회원"))
                .andExpect(jsonPath("$.data[0].content").value("즐거웠어요"))
                .andExpect(jsonPath("$.data[0].rating").value(4.5))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].writerId").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(8))
                .andExpect(jsonPath("$.meta.total").value(9))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.error").isEmpty());

        ArgumentCaptor<SmallGroupReviewSearchCondition> captor =
                ArgumentCaptor.forClass(SmallGroupReviewSearchCondition.class);
        verify(service).getSmallGroupReviews(eq(10L), isNull(), captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(1);
    }

    @Test
    void passesRequestedPageAndServerMemberInsteadOfQueryMember() throws Exception {
        when(service.getSmallGroupReviews(eq(10L), eq(40L), any()))
                .thenReturn(new SmallGroupReviewListResponse(List.of(), PageMeta.of(2, 8, 0)));

        mockMvc.perform(get("/api/small-groups/10/reviews")
                        .requestAttr("memberId", 40L).param("memberId", "999").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.size").value(8))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));

        ArgumentCaptor<SmallGroupReviewSearchCondition> captor =
                ArgumentCaptor.forClass(SmallGroupReviewSearchCondition.class);
        verify(service).getSmallGroupReviews(eq(10L), eq(40L), captor.capture());
        assertThat(captor.getValue().getPage()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid", "", "2147483648"})
    void rejectsInvalidPageBeforeService(String page) throws Exception {
        mockMvc.perform(get("/api/small-groups/10/reviews").param("page", page))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "9223372036854775808"})
    void rejectsInvalidPathBeforeService(String id) throws Exception {
        mockMvc.perform(get("/api/small-groups/" + id + "/reviews"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    // AI작성: 숫자 ID의 범위 검증은 Service에 맡기고 공통 업무 오류 응답을 유지한다.
    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void passesNonPositiveIdToServiceAndReturnsBadRequest(long id) throws Exception {
        when(service.getSmallGroupReviews(eq(id), isNull(), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "소모임 번호가 올바르지 않습니다."));

        mockMvc.perform(get("/api/small-groups/" + id + "/reviews"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verify(service).getSmallGroupReviews(eq(id), isNull(), any());
    }

    @ParameterizedTest
    @CsvSource({"403, ACCESS_DENIED", "404, NOT_FOUND", "500, INTERNAL_ERROR"})
    void preservesServiceErrorResponse(int httpStatus, String code) throws Exception {
        when(service.getSmallGroupReviews(eq(10L), eq(40L), any()))
                .thenThrow(new ApiException(HttpStatus.valueOf(httpStatus), code, "조회할 수 없습니다."));

        mockMvc.perform(get("/api/small-groups/10/reviews").requestAttr("memberId", 40L))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value("조회할 수 없습니다."));
    }
}
