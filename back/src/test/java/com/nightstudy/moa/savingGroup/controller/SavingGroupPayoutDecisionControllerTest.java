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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 기존 두 경로의 응답·입력 검증·인증 회원 전달을 단독 MockMvc로 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupPayoutDecisionControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void approveUsesJwtMemberOnly(String base) throws Exception {
        mvc.perform(patch(base + "/10/payout-requests/40/approve").requestAttr("memberId", 30L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).approveSavingGroupPayoutRequest(10L, 40L, 30L);
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void rejectUsesJwtMemberAndReason(String base) throws Exception {
        mvc.perform(patch(base + "/10/payout-requests/40/reject").requestAttr("memberId", 30L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rejectReason\":\"확인 필요\",\"memberId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).rejectSavingGroupPayoutRequest(10L, 40L, 30L, "확인 필요");
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"rejectReason\":null}", "{\"rejectReason\":\"  \"}", "broken"})
    void invalidReason(String body) throws Exception {
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/reject").requestAttr("memberId", 30L)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void reasonLengthLimit() throws Exception {
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rejectReason\":\"" + "가".repeat(501) + "\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void reasonAtLimitAccepted() throws Exception {
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/reject").requestAttr("memberId", 30L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"rejectReason\":\"" + "가".repeat(500) + "\"}"))
                .andExpect(status().isOk());
    }
    @ParameterizedTest @ValueSource(strings = {"0/payout-requests/40/approve", "10/payout-requests/0/reject", "10/payout-requests/abc/approve"})
    void invalidPath(String path) throws Exception {
        mvc.perform(patch("/api/saving-groups/" + path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rejectReason\":\"확인 필요\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409})
    void preservesErrors(int code) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(code), "PAYOUT_ERROR", "처리 불가"))
                .when(service).approveSavingGroupPayoutRequest(10L, 40L, null);
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/approve").param("memberId", "999"))
                .andExpect(status().is(code)).andExpect(jsonPath("$.error.code").value("PAYOUT_ERROR"));
    }
}
