package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.service.AdminPostService;
import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.post.dto.PostListResult;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPostController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        AdminSecurityIntegrationTest.TestSecurityBootstrap.class
})
// AI작성: 실제 SecurityFilterChain을 적용해 관리자 API의 JWT 권한별 응답을 검증한다.
class AdminSecurityIntegrationTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityBootstrap {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminPostService adminPostService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RevokedTokenMapper revokedTokenMapper;

    @MockitoBean
    private AuthSessionService authSessionService;

    @Test
    void adminApiReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/admin/posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminApiReturnsForbiddenForUserToken() throws Exception {
        mockValidToken("user-token", "USER");

        mockMvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApiAllowsAdminToken() throws Exception {
        mockValidToken("admin-token", "ADMIN");
        when(adminPostService.getPostList(any()))
                .thenReturn(new PostListResult(
                        List.of(),
                        PageMeta.of(1, 10, 0)
                ));

        mockMvc.perform(get("/api/admin/posts")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    private void mockValidToken(String token, String role) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        Date expiration = new Date(System.currentTimeMillis() + 60_000);

        when(jwtTokenProvider.parseToken(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.getId()).thenReturn("token-id");
        when(claims.get("sid", String.class))
                .thenReturn("12345678-1234-1234-1234-123456789012");
        when(claims.getExpiration()).thenReturn(expiration);
        when(revokedTokenMapper.countByTokenId("token-id")).thenReturn(0);
        when(authSessionService.isAccessAllowed(
                eq(1L),
                eq(role),
                eq("12345678-1234-1234-1234-123456789012")
        )).thenReturn(true);
    }
}
