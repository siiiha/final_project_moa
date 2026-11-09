package com.nightstudy.moa.savingGroup.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.*;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
// AI작성: 실제 MVC 바인딩·Validation·공통 응답으로 민감 필드 제외를 확인한다.
class SavingGroupTransactionControllerTest {
    SavingGroupService service = mock(SavingGroupService.class);
    org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"/api/saving-groups/2/account/transactions", "/saving-groups/2/account/transactions"})
    void responseHasOnlyApprovedFieldsAndJwtIdentity(String path) throws Exception {
        var row = new SavingGroupTransactionResponse(LocalDateTime.of(2026,11,6,12,0), "DEPOSIT", new BigDecimal("10000"), BigDecimal.ZERO);
        when(service.getSavingGroupTransactions(eq(2L),eq(1L),eq(1))).thenReturn(new SavingGroupTransactionListResponse(List.of(row),PageMeta.of(1,10,1)));
        mvc.perform(get(path).requestAttr("memberId",1L).param("memberId","999"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].amount").value(10000))
            .andExpect(jsonPath("$.data[0].balanceAfter").value(0)).andExpect(jsonPath("$.meta.size").value(10))
            .andExpect(jsonPath("$.data[0].accountNumber").doesNotExist())
            .andExpect(jsonPath("$.data[0].transactionId").doesNotExist())
            .andExpect(jsonPath("$.data[0].sourceTransactionId").doesNotExist())
            .andExpect(jsonPath("$.data[0].description").doesNotExist());
        verify(service).getSavingGroupTransactions(eq(2L),eq(1L),eq(1));
    }
    @ParameterizedTest @CsvSource({"page,0", "page,-1", "page,2147483648", "page,text"})
    void invalidQueryReturns400(String key,String value) throws Exception {
        mvc.perform(get("/api/saving-groups/2/account/transactions").param(key,value)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void invalidGroupReturns400() throws Exception {
        mvc.perform(get("/api/saving-groups/0/account/transactions")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void emptyAndExceptionsFollowCommonEnvelope() throws Exception {
        when(service.getSavingGroupTransactions(eq(2L),eq(1L),eq(1))).thenReturn(new SavingGroupTransactionListResponse(List.of(),PageMeta.of(1,10,0)));
        mvc.perform(get("/api/saving-groups/2/account/transactions").requestAttr("memberId",1L))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty()).andExpect(jsonPath("$.meta.totalPages").value(0));
        for (var status : List.of(HttpStatus.UNAUTHORIZED,HttpStatus.FORBIDDEN,HttpStatus.NOT_FOUND,HttpStatus.INTERNAL_SERVER_ERROR)) {
            when(service.getSavingGroupTransactions(eq(2L),nullable(Long.class),eq(1))).thenThrow(new ApiException(status,"TEST","조회 불가"));
            mvc.perform(get("/api/saving-groups/2/account/transactions")).andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.success").value(false));
        }
    }
}
