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
import com.nightstudy.moa.smallGroup.dto.SmallGroupVoteListItemResponse;
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
// AI작성: 투표 목록 Controller가 JWT 회원 ID·공통 응답·Service 오류를 그대로 연결하는지 검증한다.
class SmallGroupVoteListControllerTest {

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
    void getVotesUsesJwtMemberIdAndReturnsCommonResponse() throws Exception {
        when(smallGroupService.getSmallGroupVotes(10L, 30L))
                .thenReturn(List.of(voteListItem("11월 정기 모임 날짜")));

        mockMvc.perform(get("/api/small-groups/10/votes").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("11월 정기 모임 날짜"))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].endsAt").exists());

        verify(smallGroupService).getSmallGroupVotes(10L, 30L);
    }

    @Test
    void getVotesReturnsBadRequestForNonPositiveSmallGroupId() throws Exception {
        mockMvc.perform(get("/api/small-groups/0/votes").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getVotesPreservesServiceErrorResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_VOTE_LIST_FORBIDDEN",
                "승인된 소모임 회원만 투표 목록을 조회할 수 있습니다."))
                .when(smallGroupService).getSmallGroupVotes(10L, 30L);

        mockMvc.perform(get("/api/small-groups/10/votes").requestAttr("memberId", 30L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_VOTE_LIST_FORBIDDEN"));
    }

    private SmallGroupVoteListItemResponse voteListItem(String title) {
        SmallGroupVoteListItemResponse response = new SmallGroupVoteListItemResponse();
        response.setTitle(title);
        response.setStatus("OPEN");
        response.setEndsAt(LocalDateTime.of(2026, 11, 2, 18, 0));
        return response;
    }
}
