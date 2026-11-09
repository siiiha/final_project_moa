package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.SavingGroupPaymentStatusListResponse;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AI작성: 인증 요청 속성, 월 형식, 기본 페이지와 공통 빈 목록 응답을 검증한다.
class SavingGroupPaymentStatusControllerTest {
    private static final String PATH = "/api/saving-groups/2/dues/payment-status";
    private final SavingGroupService service = mock(SavingGroupService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void bindsMonthAndUsesAuthenticatedIdentity() throws Exception {
        when(service.getSavingGroupPaymentStatus(2L, 1L, YearMonth.of(2026, 11), 1))
                .thenReturn(new SavingGroupPaymentStatusListResponse(List.of(), PageMeta.of(1, 10, 0)));
        mvc.perform(get(PATH).requestAttr("memberId", 1L).param("month", "2026-11").param("memberId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.size").value(10));
    }

    @ParameterizedTest
    @CsvSource({"month,2026-13", "month,2026-1", "month,0000-01", "page,0", "page,text"})
    void rejectsInvalidQuery(String name, String value) throws Exception {
        var request = get(PATH).param(name, value);
        if (!"month".equals(name)) request.param("month", "2026-11");
        mvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
