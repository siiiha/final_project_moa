package com.nightstudy.moa.smallGroup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
// AI작성: 투표 참여·현황 Controller가 JWT 회원 ID·공통 응답·Service 업무 오류를 그대로 연결하는지 검증한다.
class SmallGroupVoteParticipationControllerTest {

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
    void participateVotePassesJwtMemberIdAndReturnsCommonSuccess() throws Exception {
        mockMvc.perform(post("/api/small-groups/10/votes/21/participations")
                        .requestAttr("memberId", 30L)
                        .contentType("application/json")
                        .content("{\"selectedOptionIds\":[101,102]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(smallGroupService).participateSmallGroupVote(
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.eq(30L), any());
    }

    @Test
    void participateVoteReturnsValidationAndServiceErrors() throws Exception {
        mockMvc.perform(post("/api/small-groups/10/votes/21/participations")
                        .requestAttr("memberId", 30L)
                        .contentType("application/json")
                        .content("{\"selectedOptionIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        doThrow(new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_VOTE_CLOSED", "마감된 투표에는 참여할 수 없습니다."))
                .when(smallGroupService).participateSmallGroupVote(
                        org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(21L),
                        org.mockito.ArgumentMatchers.eq(30L), any());

        mockMvc.perform(post("/api/small-groups/10/votes/21/participations")
                        .requestAttr("memberId", 30L)
                        .contentType("application/json")
                        .content("{\"selectedOptionIds\":[101]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_VOTE_CLOSED"));
    }

    @Test
    void getVoteStatusUsesJwtMemberIdAndReturnsDetailShape() throws Exception {
        when(smallGroupService.getSmallGroupVoteStatus(10L, 21L, 30L)).thenReturn(voteDetail());

        mockMvc.perform(get("/api/small-groups/10/votes/21/status").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.participantCount").value(2))
                .andExpect(jsonPath("$.data.options[0].voterNicknames[0]").value("걷기좋아"));

        verify(smallGroupService).getSmallGroupVoteStatus(10L, 21L, 30L);
    }

    private SmallGroupVoteDetailResponse voteDetail() {
        return new SmallGroupVoteDetailResponse(
                21L, "11월 모임 장소", "모임장", true, false,
                LocalDateTime.of(2026, 11, 1, 10, 0), LocalDateTime.of(2026, 11, 2, 18, 0),
                "OPEN", 2L, new SmallGroupVoteMyParticipationResponse(true, List.of(101L)),
                List.of(new SmallGroupVoteOptionDetailResponse(
                        101L, "한강공원", 1, 2L, List.of("걷기좋아"))));
    }
}
