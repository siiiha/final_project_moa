package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.common.dto.PageMeta;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
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

    @Test
    void publicSmallGroupDetailAllowsAnonymousRequest() throws Exception {
        SmallGroupDetailResponse response = new SmallGroupDetailResponse(
                1L, 2L, "동네 걷기", "함께 걷는 소모임입니다.", "서울", "운동",
                LocalDate.of(2026, 11, 1), "한강 공원", 10, 2L,
                "모아회원", LocalDateTime.of(2026, 10, 1, 10, 0), null, List.of());
        when(smallGroupService.getSmallGroupDetail(1L, null)).thenReturn(response);

        mockMvc.perform(get("/api/small-groups/1"))
                .andExpect(status().isOk());

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

    @Test
    void smallGroupJoinRequestApprovalApiRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/small-groups/1/join-requests/2/approve"))
                .andExpect(status().isUnauthorized());
    }
}
