package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
// AI작성: 투표 삭제 Controller가 JWT 회원 ID·경로 식별자·공통 응답과 Service 예외를 그대로 연결하는지 검증한다.
class SmallGroupVoteDeleteControllerTest {

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
    void deleteVotePassesJwtMemberIdAndReturnsCommonSuccess() throws Exception {
        mockMvc.perform(delete("/api/small-groups/10/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(smallGroupService).deleteSmallGroupVote(10L, 21L, 30L);
    }

    @Test
    void deleteVoteReturnsValidationAndServiceErrors() throws Exception {
        mockMvc.perform(delete("/api/small-groups/0/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        doThrow(new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_DELETE_FORBIDDEN",
                "투표 작성자인 소모임 개설자만 투표를 삭제할 수 있습니다."))
                .when(smallGroupService).deleteSmallGroupVote(10L, 21L, 30L);

        mockMvc.perform(delete("/api/small-groups/10/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_VOTE_DELETE_FORBIDDEN"));
    }

    @Test
    void deleteVoteReturnsNotFoundForTargetGroupMismatch() throws Exception {
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_VOTE_NOT_FOUND", "투표를 찾을 수 없습니다."))
                .when(smallGroupService).deleteSmallGroupVote(10L, 21L, 30L);

        mockMvc.perform(delete("/api/small-groups/10/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_VOTE_NOT_FOUND"));
    }
}
