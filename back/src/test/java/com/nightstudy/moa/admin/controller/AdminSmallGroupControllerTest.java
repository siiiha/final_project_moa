package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.service.AdminSmallGroupService;
import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.smallGroup.SmallGroupService;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSmallGroupController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        AdminSmallGroupControllerTest.TestSecurityBootstrap.class
})
// AI작성: 관리자 소모임 제한 API의 권한·요청 검증과 Service 위임을 실제 보안 필터로 확인한다.
class AdminSmallGroupControllerTest {
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityBootstrap {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SmallGroupService smallGroupService;

    @MockitoBean
    private AdminSmallGroupService adminSmallGroupService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RevokedTokenMapper revokedTokenMapper;

    @MockitoBean
    private AuthSessionService authSessionService;

    @Test
    void restrictionApiReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(patch("/api/admin/small-groups/1/restriction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"운영 정책 위반"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restrictionApiReturnsForbiddenForUserToken() throws Exception {
        mockValidToken("user-token", "USER");

        mockMvc.perform(patch("/api/admin/small-groups/1/restriction")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"운영 정책 위반"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void restrictionApiDelegatesValidAdminRequest() throws Exception {
        mockValidToken("admin-token", "ADMIN");

        mockMvc.perform(patch("/api/admin/small-groups/1/restriction")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"운영 정책 위반"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminSmallGroupService).restrictSmallGroup(
                eq(1L),
                argThat(request -> "운영 정책 위반".equals(request.getReason()))
        );
    }

    @Test
    void releaseApiRejectsBlankReason() throws Exception {
        mockValidToken("admin-token", "ADMIN");

        mockMvc.perform(patch("/api/admin/small-groups/1/restriction/release")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    private void mockValidToken(String token, String role) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.getId()).thenReturn("token-id");
        when(claims.get("sid", String.class))
                .thenReturn("12345678-1234-1234-1234-123456789012");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(revokedTokenMapper.countByTokenId("token-id")).thenReturn(0);
        when(authSessionService.isAccessAllowed(
                eq(1L),
                eq(role),
                eq("12345678-1234-1234-1234-123456789012")
        )).thenReturn(true);
    }
}
