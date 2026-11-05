package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupPayoutService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 금융 호출 없이 multipart 입력·인증 속성·오류 응답·비공개 증빙 헤더를 검증한다.
class SavingGroupPayoutControllerTest {
    SavingGroupPayoutService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(SavingGroupPayoutService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupPayoutController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void usesJwtAttributeAndEnvelope(String base) throws Exception {
        when(service.createPayout(eq(1L), eq(2L), any(), isNull())).thenReturn(new SavingGroupPayoutResponse("순번계", 1, BigDecimal.TEN, false, 3L, "PENDING"));
        mvc.perform(multipart(base + "/1/payout-requests").requestAttr("memberId", 2L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PENDING"));
        verify(service).createPayout(eq(1L), eq(2L), any(), isNull());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "1.1", "10000000000000"})
    void rejectsInvalidAmount(String amount) throws Exception {
        mvc.perform(multipart("/api/saving-groups/1/payout-requests").param("amount", amount))
                .andExpect(status().isBadRequest()); verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "abc"})
    void rejectsInvalidPath(String groupId) throws Exception {
        mvc.perform(multipart("/api/saving-groups/" + groupId + "/payout-requests")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409, 500})
    void preservesServiceErrors(int status) throws Exception {
        when(service.createPayout(eq(1L), isNull(), any(), isNull())).thenThrow(new ApiException(HttpStatus.valueOf(status), "PAYOUT_ERROR", "요청 실패"));
        mvc.perform(multipart("/api/saving-groups/1/payout-requests").param("memberId", "999"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("PAYOUT_ERROR"));
    }
    @Test void ownerReceiptIsNeverCachedOrServedAsHtml() throws Exception {
        when(service.getReceipt(1L, 3L, 2L)).thenReturn(new ByteArrayResource(new byte[]{1}) {
            @Override public String getFilename() { return "receipt.png"; }
        });
        mvc.perform(get("/api/saving-groups/1/payout-requests/expenses/3/receipt").requestAttr("memberId", 2L))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"receipt.png\""))
                .andExpect(content().contentType("application/octet-stream"));
    }
}
