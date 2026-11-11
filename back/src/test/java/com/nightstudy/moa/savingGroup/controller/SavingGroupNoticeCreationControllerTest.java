package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: JSON 요청 검증·인증 회원 전달·공통 성공 및 실패 응답을 기존 MockMvc 방식으로 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeCreationControllerTest {
    @Mock private SavingGroupService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // AI작성: 이미지 본문·형식·캐시 정책과 미등록 404를 확인한다.
    @Test
    void servesRepresentativeImageAndPreservesNotFound() throws Exception {
        var image = new org.springframework.core.io.ByteArrayResource(new byte[]{1, 2, 3}) {
            @Override public String getFilename() { return "sample.webp"; }
        };
        when(service.getSavingGroupImage(10L)).thenReturn(image);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/saving-groups/10/image"))
                .andExpect(status().isOk()).andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        when(service.getSavingGroupImage(10L)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "GROUP_IMAGE_NOT_FOUND", "이미지가 없습니다."));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/saving-groups/10/image"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void createsNoticeWithAuthenticatedMember(String base) throws Exception {
        mvc.perform(post(base + "/10/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
        verify(service).createSavingGroupNotice(10L, 30L, "공지", "내용");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"title\":\"공지\"}", "{\"content\":\"내용\"}",
            "{\"title\":\" \",\"content\":\"내용\"}", "{\"title\":\"공지\",\"content\":\" \\n \"}"})
    void rejectsMissingOrBlankFields(String body) throws Exception {
        mvc.perform(post("/api/saving-groups/10/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"100,5000,200", "101,10,400", "10,5001,400"})
    void validatesLengthBoundaries(int titleLength, int contentLength, int expectedStatus) throws Exception {
        mvc.perform(post("/api/saving-groups/10/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "가".repeat(titleLength) + "\",\"content\":\""
                                + "나".repeat(contentLength) + "\"}"))
                .andExpect(status().is(expectedStatus));
        if (expectedStatus == 400) {
            verifyNoInteractions(service);
        } else {
            verify(service).createSavingGroupNotice(10L, 30L, "가".repeat(titleLength), "나".repeat(contentLength));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc"})
    void rejectsInvalidPath(String id) throws Exception {
        mvc.perform(post("/api/saving-groups/" + id + "/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "null"})
    void rejectsUnreadableBody(String body) throws Exception {
        mvc.perform(post("/api/saving-groups/10/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void delegatesMissingAuthenticationToService() throws Exception {
        doThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."))
                .when(service).createSavingGroupNotice(10L, null, "공지", "내용");
        mvc.perform(post("/saving-groups/10/notices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @ParameterizedTest
    @CsvSource({"403,SAVING_GROUP_NOTICE_WRITE_FORBIDDEN", "404,SAVING_GROUP_NOT_FOUND",
            "409,SAVING_GROUP_ACTIVITY_RESTRICTED", "500,SAVING_GROUP_NOTICE_WRITE_FAILED"})
    void preservesServiceErrors(int statusCode, String code) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(statusCode), code, "요청을 처리할 수 없습니다."))
                .when(service).createSavingGroupNotice(10L, 30L, "공지", "내용");
        mvc.perform(post("/api/saving-groups/10/notices").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().is(statusCode)).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(code));
    }
}
