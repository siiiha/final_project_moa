package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.auth.*;
import com.nightstudy.moa.common.config.SecurityConfig;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.service.*;
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

// AI작성: 실제 JWT 필터·SecurityFilterChain에서 관리자 승인과 회원 계좌 조회의 인증을 검증한다.
@WebMvcTest({AdminSavingGroupCreationController.class, SavingGroupController.class})
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, SavingGroupAccountSecurityIntegrationTest.SecurityBootstrap.class})
class SavingGroupAccountSecurityIntegrationTest {
    // AI작성: 기존 보안 통합 테스트와 같이 WebMvc 슬라이스의 보안 인프라를 명시한다.
    @TestConfiguration @EnableWebSecurity static class SecurityBootstrap {}
    @Autowired MockMvc mvc;
    @MockitoBean SavingGroupCreationService creation;
    @MockitoBean SavingGroupService groups;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean AuthSessionService sessions;
    @MockitoBean RevokedTokenMapper revoked;
    void login(String role) {
        Claims claims = mock(Claims.class); Date expires = new Date(System.currentTimeMillis() + 60000);
        when(tokens.parseToken("synthetic-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1"); when(claims.get("role", String.class)).thenReturn(role);
        when(claims.getId()).thenReturn("synthetic-id"); when(claims.get("sid", String.class)).thenReturn("synthetic-session");
        when(claims.getExpiration()).thenReturn(expires);
        when(sessions.isAccessAllowed(1L, role, "synthetic-session", "synthetic-id", expires.getTime())).thenReturn(true);
    }
    @Test void anonymousCannotApproveOrReadAccount() throws Exception {
        mvc.perform(patch("/api/admin/saving-group-applications/1/approve")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/saving-groups/2/account")).andExpect(status().isUnauthorized());
        verifyNoInteractions(creation, groups);
    }
    @Test void ordinaryMemberCannotApprove() throws Exception {
        login("USER");
        mvc.perform(patch("/api/admin/saving-group-applications/1/approve").header("Authorization", "Bearer synthetic-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(creation);
    }
    @Test void adminApprovalUsesJwtIdentity() throws Exception {
        login("ADMIN");
        mvc.perform(patch("/api/admin/saving-group-applications/1/approve").header("Authorization", "Bearer synthetic-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(creation).approveCreationApplication(1L, 1L);
    }

    // AI작성: 로그인 회원에게도 Service의 모임 소속 권한 오류가 그대로 적용된다.
    @Test void authenticatedNonMemberCannotReadAccount() throws Exception {
        login("USER");
        when(groups.getSavingGroupAccount(2L, 1L)).thenThrow(new com.nightstudy.moa.common.exception.ApiException(
                org.springframework.http.HttpStatus.FORBIDDEN, "SAVING_GROUP_ACCOUNT_FORBIDDEN", "승인 모임원만 조회할 수 있습니다."));
        mvc.perform(get("/api/saving-groups/2/account").param("memberId", "999")
                        .header("Authorization", "Bearer synthetic-token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("SAVING_GROUP_ACCOUNT_FORBIDDEN"));
        verify(groups).getSavingGroupAccount(2L, 1L);
    }
}
