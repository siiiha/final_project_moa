package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 단독 MockMvc로 두 경로·JWT 속성 전달·Validation·공통 예외를 검증한다. 실제 필터 검증과 구분한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticeDeletionControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void deleteWithoutBodyUsesAuthenticatedMember(String base) throws Exception {
        mvc.perform(delete(base + "/20/notices/40").requestAttr("memberId", 30L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).deleteSavingGroupNotice(20L, 40L, 30L);
    }
    @ParameterizedTest @ValueSource(strings = {"0/notices/40", "-1/notices/40", "20/notices/0", "20/notices/abc"})
    void rejectsInvalidPath(String path) throws Exception {
        mvc.perform(delete("/saving-groups/" + path)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409, 500})
    void preservesServiceErrorsAndNeverUsesQueryMember(int status) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(status), "TEST_ERROR", "삭제 실패"))
                .when(service).deleteSavingGroupNotice(20L, 40L, null);
        mvc.perform(delete("/saving-groups/20/notices/40").param("memberId", "999"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("TEST_ERROR"));
    }
}
