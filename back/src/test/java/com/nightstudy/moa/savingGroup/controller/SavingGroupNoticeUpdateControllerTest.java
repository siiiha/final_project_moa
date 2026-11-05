package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: JWT 필터를 제외한 Controller 검증으로 경로·본문·인증 속성 전달과 공통 응답을 확인한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeUpdateControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void forwardsOnlyAuthenticatedMember(String base) throws Exception {
        when(service.updateSavingGroupNotice(20L, 40L, 30L, "제목", "내용")).thenReturn(40L);
        mvc.perform(put(base + "/20/notices/40").requestAttr("memberId", 30L).param("memberId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(40));
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"title\":\"제목\"}", "{\"title\":\" \",\"content\":\"내용\"}", "{"})
    void rejectsInvalidBody(String body) throws Exception {
        mvc.perform(put("/saving-groups/20/notices/40").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void validatesLengthBoundaries() throws Exception {
        for (int length : new int[]{100, 101}) {
            mvc.perform(put("/saving-groups/20/notices/40").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"" + "가".repeat(length) + "\",\"content\":\"내용\"}"))
                    .andExpect(status().is(length == 100 ? 200 : 400));
        }
        for (int length : new int[]{5000, 5001}) {
            mvc.perform(put("/saving-groups/20/notices/40").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"제목\",\"content\":\"" + "가".repeat(length) + "\"}"))
                    .andExpect(status().is(length == 5000 ? 200 : 400));
        }
    }
    @ParameterizedTest @ValueSource(strings = {"0/notices/40", "20/notices/-1", "abc/notices/40"})
    void rejectsInvalidPath(String path) throws Exception {
        mvc.perform(put("/saving-groups/" + path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"content\":\"내용\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409, 500})
    void preservesServiceError(int status) throws Exception {
        when(service.updateSavingGroupNotice(20L, 40L, null, "제목", "내용"))
                .thenThrow(new ApiException(HttpStatus.valueOf(status), "TEST_ERROR", "수정 실패"));
        mvc.perform(put("/saving-groups/20/notices/40").param("memberId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("TEST_ERROR"));
    }
}
