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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 역할 입력 Validation·양수 경로·JWT 속성과 기존 공통 성공/실패 응답을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberRoleControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void patchTrustsAuthenticatedActorOnly(String base) throws Exception {
        mvc.perform(patch(base + "/10/members/50/role").requestAttr("memberId", 30L).param("memberId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MANAGER\",\"memberId\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).updateSavingGroupMemberRole(10L, 50L, 30L, "MANAGER");
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"role\":null}", "{\"role\":\"\"}", "{\"role\":\"OWNER\"}",
            "{\"role\":\"ADMIN\"}", "{\"role\":\"manager\"}", "{\"role\":\" MANAGER \"}", "null", "{"})
    void invalidRoleBody(String body) throws Exception {
        mvc.perform(patch("/api/saving-groups/10/members/50/role").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(strings = {"0/members/50", "abc/members/50", "10/members/0", "10/members/abc"})
    void invalidIds(String path) throws Exception {
        mvc.perform(patch("/saving-groups/" + path + "/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409})
    void serviceErrorsArePreserved(int status) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(status), "TEST_ERROR", "변경 실패"))
                .when(service).updateSavingGroupMemberRole(10L, 50L, null, "MEMBER");
        mvc.perform(patch("/saving-groups/10/members/50/role").param("memberId", "999")
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("TEST_ERROR"));
    }
}
