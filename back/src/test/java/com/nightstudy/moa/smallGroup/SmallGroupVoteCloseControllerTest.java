package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 종료 Controller의 본문 없는 PATCH·JWT 회원 전달·공통 응답·입력 검증·Service 오류 보존을 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupVoteCloseControllerTest {
    @Mock private SmallGroupCreationService creationService;
    @Mock private SmallGroupService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SmallGroupController(creationService, service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void closeVotePassesJwtMemberIdWithoutRequestBody() throws Exception {
        mockMvc.perform(patch("/api/small-groups/10/votes/21/close").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
        verify(service).closeSmallGroupVote(10L, 21L, 30L);
    }

    // AI작성: 재요청의 무변경 판단은 Service에 위임하며 성공 응답을 그대로 반환한다.
    @Test
    void repeatedCloseReturnsCommonSuccess() throws Exception {
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(patch("/api/small-groups/10/votes/21/close").requestAttr("memberId", 30L))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        }
        verify(service, times(2)).closeSmallGroupVote(10L, 21L, 30L);
    }

    @ParameterizedTest
    @CsvSource({"0,21", "-1,21", "10,0", "10,-1", "abc,21", "10,abc", "9223372036854775808,21"})
    void invalidPathIdsAreRejectedBeforeService(String groupId, String voteId) throws Exception {
        mockMvc.perform(patch("/api/small-groups/{group}/votes/{vote}/close", groupId, voteId)
                        .requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"403,SMALL_GROUP_VOTE_CLOSE_FORBIDDEN", "404,SMALL_GROUP_NOT_FOUND",
            "404,SMALL_GROUP_VOTE_NOT_FOUND", "500,SMALL_GROUP_VOTE_CLOSE_FAILED"})
    void serviceErrorsKeepStatusCodeAndMessage(int httpStatus, String code) throws Exception {
        String message = "종료 요청 검증 오류";
        doThrow(new ApiException(HttpStatus.valueOf(httpStatus), code, message))
                .when(service).closeSmallGroupVote(10L, 21L, 30L);
        mockMvc.perform(patch("/api/small-groups/10/votes/21/close").requestAttr("memberId", 30L))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value(message));
    }

    // AI작성: 독립 Controller 테스트의 필터 없는 경로에서도 Service의 인증 누락 응답을 보존한다.
    @Test
    void missingJwtMemberIdIsPassedToServiceAndUnauthorizedIsPreserved() throws Exception {
        doThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."))
                .when(service).closeSmallGroupVote(10L, 21L, null);
        mockMvc.perform(patch("/api/small-groups/10/votes/21/close"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verify(service).closeSmallGroupVote(10L, 21L, null);
    }
}
