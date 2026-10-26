package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 가입 신청 거절 Controller가 JWT 인증 회원 ID만 전달하고 공통 오류 응답을 유지하는지 검증한다.
class SavingGroupJoinRejectionControllerTest {

    @Mock
    private SavingGroupService savingGroupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SavingGroupController(savingGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectJoinRequestUsesJwtMemberIdAndReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/saving-groups/10/join-requests/20/reject")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(savingGroupService).rejectSavingGroupJoin(10L, 20L, 30L);
    }

    @Test
    void rejectJoinRequestSupportsLegacyPathAlias() throws Exception {
        mockMvc.perform(patch("/saving-groups/10/join-requests/20/reject")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isOk());

        verify(savingGroupService).rejectSavingGroupJoin(10L, 20L, 30L);
    }

    @Test
    void rejectJoinRequestRejectsMissingAuthenticationAttribute() throws Exception {
        mockMvc.perform(patch("/api/saving-groups/10/join-requests/20/reject"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectJoinRequestReturnsBadRequestForInvalidPathValue() throws Exception {
        mockMvc.perform(patch("/api/saving-groups/not-a-number/join-requests/20/reject")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void rejectJoinRequestPreservesServiceErrorResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "JOIN_REQUEST_NOT_PENDING",
                "대기 중인 가입 신청만 거절할 수 있습니다."))
                .when(savingGroupService).rejectSavingGroupJoin(10L, 20L, 30L);

        mockMvc.perform(patch("/api/saving-groups/10/join-requests/20/reject")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("JOIN_REQUEST_NOT_PENDING"));
    }
}
