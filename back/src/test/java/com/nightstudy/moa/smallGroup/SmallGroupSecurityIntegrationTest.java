package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.common.dto.PageMeta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SmallGroupController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        SmallGroupSecurityIntegrationTest.TestSecurityBootstrap.class
})
// AI작성: SecurityFilterChain에서 공개 상세·후기 목록과 인증이 필요한 변경 API를 구분하는지 검증한다.
class SmallGroupSecurityIntegrationTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityBootstrap {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmallGroupService smallGroupService;

    @MockitoBean
    private SmallGroupCreationService smallGroupCreationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RevokedTokenMapper revokedTokenMapper;

    @MockitoBean
    private AuthSessionService authSessionService;

    // AI작성: 실제 브라우저처럼 쿼리 문자열을 URL에 포함해 공개 정규식의 회귀를 검증한다.
    @ParameterizedTest
    @ValueSource(strings = {"/api/small-groups/1", "/api/small-groups/1?noticePage=1&noticeSort=LATEST"})
    void publicSmallGroupDetailAllowsAnonymousRequest(String url) throws Exception {
        SmallGroupDetailResponse response = new SmallGroupDetailResponse(
                1L, 2L, "동네 걷기", "함께 걷는 소모임입니다.", "서울", "운동",
                LocalDate.of(2026, 11, 1), "한강 공원", 10, 2L,
                "모아회원", LocalDateTime.of(2026, 10, 1, 10, 0), null, List.of(), null, false, "UNAUTHORIZED", "로그인이 필요합니다.");
        when(smallGroupService.getSmallGroupDetail(1L, null)).thenReturn(response);
        // AI작성: 공개 상세의 공지 페이지 응답도 동일한 공개 권한으로 제공한다.
        when(smallGroupService.getSmallGroupNotices(eq(2L), any()))
                .thenReturn(new com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeListResponse(List.of(), PageMeta.of(1, 4, 0), List.of()));

        // AI작성: 비로그인 공개 상세도 신청 불가 여부와 사유를 정상 응답으로 전달한다.
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canApply").value(false))
                .andExpect(jsonPath("$.data.joinRestrictionCode").value("UNAUTHORIZED"));

        verify(smallGroupService).getSmallGroupDetail(1L, null);
    }

    // AI작성: 인증 헤더 없는 후기 GET은 필터 체인을 통과하고 memberId 없이 Service를 호출한다.
    @Test
    void publicSmallGroupReviewsAllowAnonymousRequest() throws Exception {
        when(smallGroupService.getSmallGroupReviews(eq(1L), isNull(), any()))
                .thenReturn(new SmallGroupReviewListResponse(List.of(), PageMeta.of(1, 8, 0)));

        mockMvc.perform(get("/api/small-groups/1/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.size").value(8));

        verify(smallGroupService).getSmallGroupReviews(eq(1L), isNull(), any());
    }

    @Test
    void smallGroupRegionApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/regions"))
                .andExpect(status().isUnauthorized());
    }

    // AI작성: param()만으로는 쿼리 문자열이 생성되지 않으므로 실제 URL로 첫 페이지와 추가 페이지를 검증한다.
    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void publicSmallGroupReviewsAllowAnonymousNextPage(int page) throws Exception {
        when(smallGroupService.getSmallGroupReviews(eq(1L), isNull(), any()))
                .thenReturn(new SmallGroupReviewListResponse(List.of(), PageMeta.of(page, 8, 9)));

        mockMvc.perform(get("/api/small-groups/1/reviews?page=" + page))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(page));

        var condition = org.mockito.ArgumentCaptor.forClass(
                com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition.class);
        verify(smallGroupService).getSmallGroupReviews(eq(1L), isNull(), condition.capture());
        org.assertj.core.api.Assertions.assertThat(condition.getValue().getPage()).isEqualTo(page);
        verifyNoInteractions(jwtTokenProvider, authSessionService, revokedTokenMapper);
    }

    // AI작성: 쿼리 허용이 하위 경로·비숫자 ID·인증 전용 API까지 공개하지 않는지 확인한다.
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/small-groups/1/reviews/me?page=1",
            "/api/small-groups/1/settings?noticePage=1",
            "/api/small-groups/1/votes?page=1",
            "/api/small-groups/regions?noticePage=1",
            "/api/small-groups/abc?noticePage=1",
            "/api/small-groups/1/reviews/9?page=1"
    })
    void queryStringDoesNotExposeProtectedPaths(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
        verifyNoInteractions(smallGroupService);
    }

    // AI작성: 공개 목록과 동일한 쿼리가 있어도 POST 작성 요청은 인증이 필요하다.
    @Test
    void reviewCreateWithQueryStillRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/small-groups/1/reviews?page=1")
                        .contentType("application/json").content("{\"rating\":4.5,\"content\":\"즐거웠어요\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(smallGroupService);
    }

    // AI작성: 목록 공개 설정이 후기 개별 경로나 수정·삭제 요청으로 확대되지 않는지 검증한다.
    @Test
    void reviewItemAndMutationsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/reviews/9"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/small-groups/1/reviews/9")
                        .contentType("application/json").content("{\"content\":\"수정\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/small-groups/1/reviews/9")
                        .contentType("application/json").content("{\"content\":\"수정\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/small-groups/1/reviews/9"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(smallGroupService);
    }

    // AI작성: 공개 후기 GET 경로가 본인 작성 여부 조회까지 공개하지 않는지 확인한다.
    @Test
    void ownReviewStatusRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/reviews/me").requestAttr("memberId", 40L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(smallGroupService);
    }

    // AI작성: 인증 속성을 임의로 전달해도 SecurityFilterChain의 로그인 요구를 우회할 수 없다.
    @Test
    void smallGroupReviewCreateRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/small-groups/1/reviews")
                        .requestAttr("memberId", 40L)
                        .contentType("application/json")
                        .content("{\"rating\":4.5,\"content\":\"즐거웠어요\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(smallGroupService);
    }

    @Test
    void smallGroupSettingsApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupSettingsUpdateApiRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/small-groups/1")
                        .contentType("application/json")
                        .content("{\"groupName\":\"주말 걷기\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupJoinRequestApiRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/small-groups/1/join-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupJoinRequestListApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/join-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupVoteListApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/votes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupVoteParticipationApiRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/small-groups/1/votes/2/participations")
                        .contentType("application/json")
                        .content("{\"selectedOptionIds\":[101]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupVoteStatusApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/votes/2/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void smallGroupVoteDeleteApiRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/small-groups/1/votes/2"))
                .andExpect(status().isUnauthorized());
    }

    // AI작성: 인증 속성 위조만으로 종료 API에 접근할 수 없고 Service도 호출되지 않는다.
    @Test
    void smallGroupVoteCloseApiRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/small-groups/1/votes/2/close").requestAttr("memberId", 40L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_REQUIRED"));
        verifyNoInteractions(smallGroupService);
    }

    // AI작성: 실제 서명 토큰 대신 가상 Claims로 필터를 통과시키고 JWT 회원 ID가 요청 속성을 덮어쓰는지 확인한다.
    @Test
    void authenticatedVoteCloseUsesJwtMemberId() throws Exception {
        Claims claims = mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 60000);
        when(jwtTokenProvider.parseToken("mock-close-request")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("40");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("mock-close-id");
        when(claims.get("sid", String.class)).thenReturn("mock-close-session");
        when(claims.getExpiration()).thenReturn(expiration);
        when(authSessionService.isAccessAllowed(40L, "USER", "mock-close-session", "mock-close-id", expiration.getTime()))
                .thenReturn(true);
        mockMvc.perform(patch("/api/small-groups/1/votes/2/close")
                        .header("Authorization", "Bearer mock-close-request")
                        .requestAttr("memberId", 99L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        verify(smallGroupService).closeSmallGroupVote(1L, 2L, 40L);
    }

    @Test
    void smallGroupJoinRequestApprovalApiRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/small-groups/1/join-requests/2/approve"))
                .andExpect(status().isUnauthorized());
    }

    // AI작성: 공개 요약 경로가 공지 본문·변경 API까지 공개하지 않는지 실제 보안 필터로 검증한다.
    @Test
    void noticeBodyAndMutationsRequireJwt() throws Exception {
        mockMvc.perform(get("/api/small-groups/1/notices/9")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/small-groups/1/notices").requestAttr("memberId", 40L)
                .contentType("application/json").content("{\"title\":\"공지\",\"content\":\"본문\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/small-groups/1/notices/9")
                .contentType("application/json").content("{\"title\":\"공지\",\"content\":\"본문\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/small-groups/1/notices/9")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/small-groups/1/notices/9/pin").contentType("application/json").content("{\"pinned\":true}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(smallGroupService);
    }
}
