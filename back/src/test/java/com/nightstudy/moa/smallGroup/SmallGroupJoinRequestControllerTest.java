package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestRow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 가입 신청 목록 Controller가 JWT 회원 ID·쿼리 조건만 Service에 전달하고 공통 응답을 반환하는지 검증한다.
class SmallGroupJoinRequestControllerTest {

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
    void getJoinRequestsUsesJwtMemberIdAndReturnsDataAndPageMeta() throws Exception {
        when(smallGroupService.getSmallGroupJoinRequests(10L, 30L, "PENDING", 2))
                .thenReturn(new SmallGroupJoinRequestListResponse(
                        List.of(joinRequestRow()), PageMeta.of(2, 10, 11)));

        mockMvc.perform(get("/api/small-groups/10/join-requests")
                        .requestAttr("memberId", 30L)
                        .param("status", "PENDING")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].crewMemberId").value("9007199254740993"))
                .andExpect(jsonPath("$.data[0].applicant").value("가**"))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.meta.total").value(11));

        verify(smallGroupService).getSmallGroupJoinRequests(10L, 30L, "PENDING", 2);
    }

    @Test
    void getJoinRequestsRejectsMissingAuthenticationAttribute() throws Exception {
        mockMvc.perform(get("/api/small-groups/10/join-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getJoinRequestsPreservesServiceErrorResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_OWNER_REQUIRED",
                "소모임 개설자만 가입 신청을 조회할 수 있습니다."))
                .when(smallGroupService).getSmallGroupJoinRequests(10L, 30L, null, 1);

        mockMvc.perform(get("/api/small-groups/10/join-requests").requestAttr("memberId", 30L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_OWNER_REQUIRED"));
    }

    private SmallGroupJoinRequestRow joinRequestRow() {
        SmallGroupJoinRequestRow row = new SmallGroupJoinRequestRow();
        row.setCrewMemberId("9007199254740993");
        row.setApplicant("가**");
        row.setRegionName("서울");
        row.setIdentityVerified(true);
        row.setRegionVerified(true);
        row.setApplicationMessage("함께하고 싶습니다.");
        row.setMembershipStatus("PENDING");
        return row;
    }
}
