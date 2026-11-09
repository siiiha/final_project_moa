package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupTurnOrderService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 두 경로 별칭과 검증·인증 속성 전달 및 공통 응답을 실제 MVC로 확인한다.
class SavingGroupTurnOrderAssignmentControllerTest {
    SavingGroupTurnOrderService service; MockMvc mvc;
    @BeforeEach void setup() {
        service = mock(SavingGroupTurnOrderService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupTurnOrderController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings={"/api/saving-groups", "/saving-groups"})
    void updatesWithJwtAttributeAndReturnsCommonResponse(String prefix) throws Exception {
        when(service.update(eq(10L),eq(30L),any())).thenReturn(new SavingGroupTurnOrderResponse(LocalDate.of(2026,12,1),3,true,false,List.of()));
        mvc.perform(put(prefix+"/10/turn-orders").requestAttr("memberId",30L).contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnOrders\":[{\"memberId\":30,\"sequenceNumber\":3}]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.effectiveMonth").value("2026-12-01"));
        verify(service).update(eq(10L),eq(30L),any());
    }
    @Test void rejectsInvalidBodyBeforeService() throws Exception {
        for (String body : List.of("{}", "{\"turnOrders\":[]}","{\"turnOrders\":[null]}","{\"turnOrders\":[{\"memberId\":0,\"sequenceNumber\":-1}]}"))
            mvc.perform(put("/api/saving-groups/10/turn-orders").requestAttr("memberId",30L).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void queryParameterCannotSupplyAuthenticatedIdentity() throws Exception {
        when(service.get(10L,null)).thenThrow(new com.nightstudy.moa.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","로그인이 필요합니다."));
        mvc.perform(get("/api/saving-groups/10/turn-orders").param("memberId","30")).andExpect(status().isUnauthorized());
        verify(service).get(10L,null);
    }
}
