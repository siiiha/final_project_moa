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

// AI작성: 실제 JWT 필터·SecurityFilterChain에서 거래내역 조회 인증을 검증한다.
@WebMvcTest(SavingGroupController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, SavingGroupTransactionSecurityTest.SecurityBootstrap.class})
class SavingGroupTransactionSecurityTest {
    // AI작성: 기존 보안 통합 테스트와 같이 WebMvc 슬라이스의 보안 인프라를 명시한다.
    @TestConfiguration @EnableWebSecurity static class SecurityBootstrap {}
    @Autowired MockMvc mvc;
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
    // AI작성: 익명 요청은 두 별칭 모두 금융 응답을 받지 못한다.
    @Test void anonymousCannotReadTransactions() throws Exception {
        // AI작성: /api 없는 별칭은 기존 보안 설정상 Service의 로그인 검사에서 차단된다.
        when(groups.getSavingGroupTransactions(eq(2L), isNull(), any())).thenThrow(
            new com.nightstudy.moa.common.exception.ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "로그인이 필요합니다."));
        mvc.perform(get("/api/saving-groups/2/account/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/saving-groups/2/account/transactions")).andExpect(status().isUnauthorized());
    }
    // AI작성: 인증된 회원 번호는 쿼리 입력보다 JWT 속성을 우선한다.
    @Test void authenticatedMemberUsesJwtIdentity() throws Exception {
        login("USER");
        when(groups.getSavingGroupTransactions(eq(2L),eq(1L),any())).thenReturn(
            new com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionListResponse(java.util.List.of(),
                com.nightstudy.moa.common.dto.PageMeta.of(1,10,0)));
        mvc.perform(get("/api/saving-groups/2/account/transactions").param("memberId","999")
            .header("Authorization","Bearer synthetic-token"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        verify(groups).getSavingGroupTransactions(eq(2L),eq(1L),any());
    }

    // AI작성: 로그인 회원에게도 Service의 모임 소속 권한 오류가 그대로 적용된다.
    @Test void authenticatedNonMemberCannotReadAccount() throws Exception {
        login("USER");
        when(groups.getSavingGroupTransactions(eq(2L), eq(1L), any())).thenThrow(new com.nightstudy.moa.common.exception.ApiException(
                org.springframework.http.HttpStatus.FORBIDDEN, "SAVING_GROUP_ACCOUNT_FORBIDDEN", "승인 모임원만 조회할 수 있습니다."));
        mvc.perform(get("/api/saving-groups/2/account/transactions").param("memberId", "999")
                        .header("Authorization", "Bearer synthetic-token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("SAVING_GROUP_ACCOUNT_FORBIDDEN"));
        verify(groups).getSavingGroupTransactions(eq(2L), eq(1L), any());
    }
}
