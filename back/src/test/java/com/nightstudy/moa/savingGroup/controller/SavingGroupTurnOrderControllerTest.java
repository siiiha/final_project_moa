package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.dto.SavingGroupTurnOrderRecommendationResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 두 경로의 응답·인증 속성·캐시 금지·공통 오류 처리를 확인한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupTurnOrderControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void responseUsesOnlyAuthenticatedIdentity(String base) throws Exception {
        when(service.getTurnOrderRecommendations(10L, 30L)).thenReturn(List.of(
                new SavingGroupTurnOrderRecommendationResponse("9223372036854775807", "테스트계주", 1, new BigDecimal("3297"))));
        mvc.perform(get(base + "/10/turn-orders/recommendations").requestAttr("memberId", 30L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].memberId").value("9223372036854775807"))
                .andExpect(jsonPath("$.data[0].nickname").value("테스트계주"))
                .andExpect(jsonPath("$.data[0].recommendedTurnOrder").value(1))
                .andExpect(jsonPath("$.data[0].expectedPayoutAmount").value(3297));
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "abc"})
    void invalidPath(String id) throws Exception {
        mvc.perform(get("/api/saving-groups/" + id + "/turn-orders/recommendations"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409})
    void errorsPreserveExistingFormat(int status) throws Exception {
        when(service.getTurnOrderRecommendations(10L, null))
                .thenThrow(new ApiException(HttpStatus.valueOf(status), "POLICY_ERROR", "조회 불가"));
        mvc.perform(get("/api/saving-groups/10/turn-orders/recommendations").param("memberId", "30"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("POLICY_ERROR"));
    }
}
