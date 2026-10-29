package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
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
// AI작성: SecurityFilterChain에서 숫자 ID 소모임 상세만 비로그인 공개인지 검증한다.
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

    @Test
    void smallGroupRegionApiStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/small-groups/regions"))
                .andExpect(status().isUnauthorized());
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
}
