package com.nightstudy.moa.report.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SecurityErrorResponseWriter;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;
import com.nightstudy.moa.report.dto.ReportCreateRequest;
import com.nightstudy.moa.report.service.ReportService;
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

@WebMvcTest(ReportController.class)
@Import({
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        ReportControllerTest.TestSecurityBootstrap.class
})
// AI작성: 회원 신고 API의 로그인 인증, 요청 검증, Service 위임을 실제 보안 필터로 확인한다.
class ReportControllerTest {
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityBootstrap {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RevokedTokenMapper revokedTokenMapper;

    @MockitoBean
    private AuthSessionService authSessionService;

    @Test
    void createReportReturnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"POST","targetId":10,"reason":"광고성 게시글입니다."}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReportDelegatesAuthenticatedRequest() throws Exception {
        mockValidUserToken();
        when(reportService.createReport(eq(1L), any(ReportCreateRequest.class)))
                .thenReturn(20L);

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"POST","targetId":10,"reason":"광고성 게시글입니다."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(20));

        verify(reportService).createReport(
                eq(1L),
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.getTargetId().equals(10L)
                                && "광고성 게시글입니다.".equals(request.getReason()))
        );
    }

    @Test
    void createReportRejectsBlankReason() throws Exception {
        mockValidUserToken();

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"POST","targetId":10,"reason":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    private void mockValidUserToken() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenProvider.parseToken("user-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("token-id");
        when(claims.get("sid", String.class))
                .thenReturn("12345678-1234-1234-1234-123456789012");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(revokedTokenMapper.countByTokenId("token-id")).thenReturn(0);
        when(authSessionService.isAccessAllowed(
                1L,
                "USER",
                "12345678-1234-1234-1234-123456789012"
        )).thenReturn(true);
    }
}
