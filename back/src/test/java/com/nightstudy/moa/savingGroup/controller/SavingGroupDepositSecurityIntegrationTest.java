package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.auth.*;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.service.SavingGroupDepositService;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

// AI작성: 실제 보안 체인에서 두 입금 경로의 미인증 차단과 JWT 회원 전달을 검증한다.
@WebMvcTest(SavingGroupDepositController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, SavingGroupDepositSecurityIntegrationTest.SecurityBootstrap.class})
class SavingGroupDepositSecurityIntegrationTest {
    @TestConfiguration @EnableWebSecurity static class SecurityBootstrap {}
    @Autowired MockMvc mvc;
    @MockitoBean SavingGroupDepositService service;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean AuthSessionService sessions;
    @MockitoBean RevokedTokenMapper revoked;
    @Test void anonymousCannotDepositOrConfirm() throws Exception {
        for (String suffix : new String[]{"", "/confirm"}) {
            mvc.perform(post("/api/saving-groups/10/account/deposits" + suffix).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }
    @Test void authenticatedRequestUsesJwtSubject() throws Exception {
        Claims claims = mock(Claims.class); Date expiry = new Date(System.currentTimeMillis() + 60000);
        when(tokens.parseToken("synthetic-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("20"); when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("synthetic-id"); when(claims.get("sid", String.class)).thenReturn("synthetic-session");
        when(claims.getExpiration()).thenReturn(expiry);
        when(sessions.isAccessAllowed(20L, "USER", "synthetic-session", "synthetic-id", expiry.getTime())).thenReturn(true);
        mvc.perform(post("/api/saving-groups/10/account/deposits").header("Authorization", "Bearer synthetic-token")
                .param("memberId", "999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10,\"requestKey\":\"mock-request-key-0001\"}"))
                .andExpect(status().isOk());
        verify(service).deposit(eq(10L), eq(20L), any());
    }
}
