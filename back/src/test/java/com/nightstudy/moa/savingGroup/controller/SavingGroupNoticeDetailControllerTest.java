package com.nightstudy.moa.savingGroup.controller;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.SavingGroupNoticeDetailResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.time.LocalDateTime;
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

// AI작성: 기존 standalone MockMvc로 요청 전달과 응답을 검증한다. 실제 JWT 필터 동작은 이 테스트 범위가 아니다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeDetailControllerTest {
    @Mock private SavingGroupService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void returnsDetailWithAuthenticatedMember(String base) throws Exception {
        when(service.getSavingGroupNoticeDetail(20L, 40L, 30L)).thenReturn(
                new SavingGroupNoticeDetailResponse(40L, "공지 제목", "공지 내용", 50L, "테스트닉네임",
                        LocalDateTime.of(2026, 11, 4, 10, 0)));
        // AI작성: 쿼리의 memberId는 무시하고 인증 계층의 요청 속성만 Service에 전달한다.
        mvc.perform(get(base + "/20/notices/40").requestAttr("memberId", 30L).param("memberId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", aMapWithSize(6)))
                .andExpect(jsonPath("$.data.noticeId").value(40))
                .andExpect(jsonPath("$.data.title").value("공지 제목"))
                .andExpect(jsonPath("$.data.content").value("공지 내용"))
                .andExpect(jsonPath("$.data.writerId").value(50))
                .andExpect(jsonPath("$.data.writerNickname").value("테스트닉네임"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());
        verify(service).getSavingGroupNoticeDetail(20L, 40L, 30L);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"0,40", "-1,40", "abc,40", "9223372036854775808,40",
            "20,0", "20,-1", "20,abc", "20,9223372036854775808"})
    void rejectsInvalidPathIds(String groupId, String noticeId) throws Exception {
        mvc.perform(get("/api/saving-groups/" + groupId + "/notices/" + noticeId).requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void delegatesMissingAuthenticationToService(String base) throws Exception {
        when(service.getSavingGroupNoticeDetail(20L, 40L, null))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."));
        mvc.perform(get(base + "/20/notices/40").param("memberId", "30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verify(service).getSavingGroupNoticeDetail(20L, 40L, null);
    }

    @ParameterizedTest
    @CsvSource({"403,SAVING_GROUP_NOTICE_READ_FORBIDDEN", "404,NOT_FOUND",
            "404,SAVING_GROUP_NOTICE_NOT_FOUND", "500,INTERNAL_ERROR"})
    void preservesServiceErrors(int statusCode, String code) throws Exception {
        when(service.getSavingGroupNoticeDetail(20L, 40L, 30L))
                .thenThrow(new ApiException(HttpStatus.valueOf(statusCode), code, "요청을 처리할 수 없습니다."));
        mvc.perform(get("/api/saving-groups/20/notices/40").requestAttr("memberId", 30L))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value("요청을 처리할 수 없습니다."));
    }
}
