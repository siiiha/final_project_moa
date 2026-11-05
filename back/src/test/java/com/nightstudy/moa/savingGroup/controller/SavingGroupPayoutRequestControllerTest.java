package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 MVC 바인딩·공통 예외·유형별 응답 공개 범위를 검증한다. JWT 필터 검증과는 구분한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupPayoutRequestControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void existingAliasesAndEmptyEnvelope(String prefix) throws Exception {
        when(service.getSavingGroupPayoutRequests(10L, 30L, null, 1))
                .thenReturn(new SavingGroupPayoutRequestListResponse(List.of(), PageMeta.of(1, 10, 0)));
        mvc.perform(get(prefix + "/10/payout-requests").requestAttr("memberId", 30L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty()).andExpect(jsonPath("$.meta.totalPages").value(0));
    }
    @ParameterizedTest @ValueSource(strings = {"page=0", "page=-1", "page=abc", "page=", "status=UNKNOWN", "status="})
    void invalidQueryFailsValidation(String query) throws Exception {
        mvc.perform(get("/api/saving-groups/10/payout-requests?" + query).requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void missingAuthenticationUsesServiceError() throws Exception {
        when(service.getSavingGroupPayoutRequests(10L, null, null, 1))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."));
        mvc.perform(get("/api/saving-groups/10/payout-requests"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
    @ParameterizedTest @ValueSource(ints = {403, 404, 409})
    void serviceFailuresKeepStatus(int code) throws Exception {
        when(service.getSavingGroupPayoutRequests(10L, 30L, null, 1))
                .thenThrow(new ApiException(HttpStatus.valueOf(code), "EXISTING_ERROR", "조회할 수 없습니다."));
        mvc.perform(get("/api/saving-groups/10/payout-requests").requestAttr("memberId", 30L))
                .andExpect(status().is(code)).andExpect(jsonPath("$.success").value(false));
    }
    @Test void socialResponseHasEvidenceFlagWithoutRawSensitiveFields() throws Exception {
        var row = new SavingGroupPayoutRequestResponse();
        row.setRequestId("9223372036854775807"); row.setRequesterName("테스트회원");
        row.setAmount(new BigDecimal("1234.50")); row.setTitle("모임 다과"); row.setHasEvidence(true);
        row.setStatus("PENDING");
        when(service.getSavingGroupPayoutRequests(10L, 30L, "PENDING", 2))
                .thenReturn(new SavingGroupPayoutRequestListResponse(List.of(row), PageMeta.of(2, 10, 11)));
        mvc.perform(get("/api/saving-groups/10/payout-requests?status=PENDING&page=2").requestAttr("memberId", 30L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].requestId").value("9223372036854775807"))
                .andExpect(jsonPath("$.data[0].requesterName").value("테스트회원"))
                .andExpect(jsonPath("$.data[0].hasEvidence").value(true))
                .andExpect(jsonPath("$.data[0].roundNumber").doesNotExist())
                .andExpect(jsonPath("$.data[0].receiptImgUrl").doesNotExist())
                .andExpect(jsonPath("$.data[0].transactionId").doesNotExist())
                .andExpect(jsonPath("$.data[0].accountNumber").doesNotExist());
    }
}
