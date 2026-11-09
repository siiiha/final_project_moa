package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupAccountResponse;
import com.nightstudy.moa.common.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;

// AI작성: 일반 응답은 마스킹 계좌번호만 포함하며 인증 속성을 Service에 전달한다.
class SavingGroupAccountControllerTest {
    @Test void returnsOnlyMaskedInformation() throws Exception {
        var service = mock(SavingGroupService.class);
        when(service.getSavingGroupAccount(2L, 1L)).thenReturn(new SavingGroupAccountResponse(true, "00", "********0000", BigDecimal.ZERO, true));
        var mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/saving-groups/2/account").requestAttr("memberId", 1L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.maskedAccountNumber").value("********0000"))
                .andExpect(jsonPath("$.data.accountNumber").doesNotExist()).andExpect(jsonPath("$.data.accountHolderName").doesNotExist());
        verify(service).getSavingGroupAccount(2L, 1L);
        mvc.perform(get("/api/saving-groups/0/account")).andExpect(status().isBadRequest());
    }
}
