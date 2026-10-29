package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 승인 Controller가 서버 인증 회원 ID만 전달하고 공통 성공·오류 응답을 유지하는지 검증한다.
class SmallGroupJoinApprovalControllerTest {

    @Mock
    private SmallGroupCreationService smallGroupCreationService;

    @Mock
    private SmallGroupService smallGroupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SmallGroupController(smallGroupCreationService, smallGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void approveJoinRequestUsesJwtMemberIdAndReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/small-groups/10/join-requests/20/approve")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(smallGroupService).approveSmallGroupJoin(10L, 20L, 30L);
    }

    @Test
    void approveJoinRequestRejectsMissingAuthenticationAttribute() throws Exception {
        mockMvc.perform(patch("/api/small-groups/10/join-requests/20/approve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void approveJoinRequestReturnsBadRequestForInvalidPathValue() throws Exception {
        mockMvc.perform(patch("/api/small-groups/not-a-number/join-requests/20/approve")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void approveJoinRequestPreservesServiceErrorResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                "소모임 개설자만 가입 신청을 승인할 수 있습니다."))
                .when(smallGroupService).approveSmallGroupJoin(10L, 20L, 30L);

        mockMvc.perform(patch("/api/small-groups/10/join-requests/20/approve")
                        .requestAttr("memberId", 30L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_OWNER_REQUIRED"));
    }
}
