package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 기존 공개 상세 경로의 조건 바인딩·검증·data/meta 응답을 검증하며 실제 JWT·DB는 실행하지 않는다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticePagingControllerTest {
    @Mock private SavingGroupService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void defaultQueryRemainsPublicAndUsesLatestFirstPage(String base) throws Exception {
        when(service.getSavingGroupDetail(eq(20L), isNull(), argThat(condition ->
                condition.getNoticePage() == 1 && "LATEST".equals(condition.getNoticeSort()))))
                .thenReturn(result(1, 9));

        // AI작성: 임의의 memberId 쿼리는 인증 정보로 사용하지 않는다.
        mvc.perform(get(base + "/20").param("memberId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupId").value(20))
                .andExpect(jsonPath("$.data.savingGroupId").value(10))
                .andExpect(jsonPath("$.data.notices").isArray())
                .andExpect(jsonPath("$.data.detail").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(4))
                .andExpect(jsonPath("$.meta.total").value(9))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.error").isEmpty());
        verify(service).getSavingGroupDetail(eq(20L), isNull(), any(SavingGroupNoticeSearchCondition.class));
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LATEST", "OLDEST"})
    void forwardsRequestedPageSortAndAuthenticatedMember(String sort) throws Exception {
        when(service.getSavingGroupDetail(eq(20L), eq(30L), argThat(condition ->
                condition.getNoticePage() == 2 && sort.equals(condition.getNoticeSort()))))
                .thenReturn(result(2, 9));

        mvc.perform(get("/api/saving-groups/20").requestAttr("memberId", 30L)
                        .param("memberId", "999").param("noticePage", "2").param("noticeSort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(2));
        verify(service).getSavingGroupDetail(eq(20L), eq(30L), any(SavingGroupNoticeSearchCondition.class));
    }

    @ParameterizedTest
    @CsvSource({"noticePage,0", "noticePage,-1", "noticePage,abc", "noticePage,1.5",
            "noticePage,2147483648", "noticePage,''", "noticeSort,latest", "noticeSort,UNKNOWN", "noticeSort,''"})
    void rejectsInvalidQueryBeforeServiceCall(String key, String value) throws Exception {
        mvc.perform(get("/saving-groups/20").param(key, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void rejectsInvalidGroupId(String id) throws Exception {
        mvc.perform(get("/saving-groups/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"1,0,0", "4,9,3"})
    void returnsEmptyNoticesWithServiceMeta(int page, int total, int totalPages) throws Exception {
        when(service.getSavingGroupDetail(eq(20L), isNull(), any(SavingGroupNoticeSearchCondition.class)))
                .thenReturn(result(page, total));
        mvc.perform(get("/saving-groups/20").param("noticePage", Integer.toString(page)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notices").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(page))
                .andExpect(jsonPath("$.meta.total").value(total))
                .andExpect(jsonPath("$.meta.totalPages").value(totalPages));
    }

    @ParameterizedTest
    @CsvSource({"404,NOT_FOUND", "500,INTERNAL_ERROR"})
    void preservesServiceErrors(int statusCode, String code) throws Exception {
        when(service.getSavingGroupDetail(eq(20L), isNull(), any(SavingGroupNoticeSearchCondition.class)))
                .thenThrow(new ApiException(HttpStatus.valueOf(statusCode), code, "요청을 처리할 수 없습니다."));
        mvc.perform(get("/saving-groups/20"))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error.code").value(code));
    }

    private SavingGroupDetailPageResponse result(int page, int total) {
        // AI작성: 실제 상세 응답을 사용해 내부 결과 DTO가 data 안에 중첩되지 않는지 확인한다.
        var detail = new SavingGroupDetailResponse(10L, 20L, "테스트 계모임", "설명", "서울", 10,
                2L, null, null, "01", List.of(), null, null, false, "UNAUTHORIZED", "로그인이 필요합니다.");
        return new SavingGroupDetailPageResponse(detail, PageMeta.of(page, 4, total));
    }
}
