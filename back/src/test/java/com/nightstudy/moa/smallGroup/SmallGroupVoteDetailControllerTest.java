package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteMyParticipationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteOptionDetailResponse;
import java.time.LocalDateTime;
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
// AI작성: 투표 상세 Controller가 JWT 회원 ID·공통 응답·Service 업무 오류를 그대로 연결하는지 검증한다.
class SmallGroupVoteDetailControllerTest {

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
    void getVoteDetailUsesJwtMemberIdAndReturnsCommonResponse() throws Exception {
        when(smallGroupService.getSmallGroupVoteDetail(10L, 21L, 30L)).thenReturn(voteDetail());

        mockMvc.perform(get("/api/small-groups/10/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.smallGroupVoteId").value(21))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.participantCount").value(2))
                .andExpect(jsonPath("$.data.myParticipation.participated").value(true))
                .andExpect(jsonPath("$.data.options[0].content").value("한강공원"));

        verify(smallGroupService).getSmallGroupVoteDetail(10L, 21L, 30L);
    }

    @Test
    void getVoteDetailReturnsBadRequestForNonPositivePathVariables() throws Exception {
        mockMvc.perform(get("/api/small-groups/0/votes/21").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/small-groups/10/votes/0").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getVoteDetailPreservesUnauthorizedAndNotFoundServiceErrors() throws Exception {
        doThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."))
                .when(smallGroupService).getSmallGroupVoteDetail(10L, 21L, null);
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_VOTE_NOT_FOUND", "투표를 찾을 수 없습니다."))
                .when(smallGroupService).getSmallGroupVoteDetail(10L, 22L, 30L);

        mockMvc.perform(get("/api/small-groups/10/votes/21"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/small-groups/10/votes/22").requestAttr("memberId", 30L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_VOTE_NOT_FOUND"));
    }

    private SmallGroupVoteDetailResponse voteDetail() {
        return new SmallGroupVoteDetailResponse(
                21L,
                "11월 모임 장소",
                "모임장",
                true,
                false,
                LocalDateTime.of(2026, 10, 30, 10, 0),
                LocalDateTime.of(2026, 10, 31, 18, 0),
                "OPEN",
                2L,
                new SmallGroupVoteMyParticipationResponse(true, List.of(101L)),
                List.of(new SmallGroupVoteOptionDetailResponse(
                        101L, "한강공원", 1, 2L, List.of("걷기좋아", "모아회원"))));
    }
}
