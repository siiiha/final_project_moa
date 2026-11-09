package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupDepositService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 사용자 입력 ID 대신 인증 속성 전달, 본문 검증, 공통 오류·응답 형식을 검증한다.
class SavingGroupDepositControllerTest {
    @Test void usesAuthenticatedIdentityAndExistingEnvelope() throws Exception {
        var service = mock(SavingGroupDepositService.class);
        when(service.deposit(eq(10L), eq(20L), any())).thenReturn(new SavingGroupDepositResponse(
                "COMPLETED", BigDecimal.TEN, BigDecimal.TEN, null, null, "테스트 입금이 등록되었습니다."));
        var mvc = MockMvcBuilders.standaloneSetup(new SavingGroupDepositController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String prefix : new String[]{"/api", ""}) {
            mvc.perform(post(prefix + "/saving-groups/10/account/deposits").requestAttr("memberId", 20L)
                    .param("memberId", "999").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":10,\"requestKey\":\"mock-request-key-0001\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.accountNumber").doesNotExist())
                    .andExpect(jsonPath("$.data.transactionId").doesNotExist());
        }
        verify(service, times(2)).deposit(eq(10L), eq(20L), any());
    }
    @Test void rejectsInvalidInputBeforeService() throws Exception {
        var service = mock(SavingGroupDepositService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new SavingGroupDepositController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String amount : new String[]{"null", "0", "-1", "1.01", "10000000000000"}) {
            mvc.perform(post("/api/saving-groups/10/account/deposits").requestAttr("memberId", 20L)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + ",\"requestKey\":\"mock-request-key-0001\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/saving-groups/0/account/deposits").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10,\"requestKey\":\"mock-request-key-0001\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/saving-groups/10/account/deposits/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestKey\":\"mock-request-key-0001\",\"confirmationToken\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void preservesServiceExceptionOnConfirm() throws Exception {
        var service = mock(SavingGroupDepositService.class);
        when(service.confirm(eq(10L), eq(20L), any())).thenThrow(new ApiException(
                HttpStatus.CONFLICT, "DEPOSIT_CONFIRMATION_EXPIRED", "확인 시간이 지났습니다."));
        var mvc = MockMvcBuilders.standaloneSetup(new SavingGroupDepositController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/saving-groups/10/account/deposits/confirm").requestAttr("memberId", 20L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"requestKey\":\"mock-request-key-0001\",\"confirmationToken\":\"" + "A".repeat(43) + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DEPOSIT_CONFIRMATION_EXPIRED"));
    }
}
