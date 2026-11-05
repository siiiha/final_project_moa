package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.auth.*;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupServiceImpl;
import com.nightstudy.moa.savingGroup.dto.*;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

// AI작성: 실제 JWT 필터→Controller→Service를 연결한다. 서명 파서·인증 세션·DB는 대역이며 전체 SecurityFilterChain 테스트는 아니다.
@ExtendWith(MockitoExtension.class)
class SavingGroupPayoutDecisionSecurityTest {
    @Mock JwtTokenProvider provider;
    @Mock RevokedTokenMapper revoked;
    @Mock AuthSessionService sessions;
    @Mock SavingGroupMapper mapper;
    MockMvc mvc;
    @BeforeEach void setup() {
        var filter = new JwtAuthenticationFilter(provider, new SecurityErrorResponseWriter(new ObjectMapper()), revoked, sessions);
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(new SavingGroupServiceImpl(mapper)))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(filter).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @ParameterizedTest @ValueSource(strings = {"approve", "reject"})
    void anonymousCannotUseQueryMember(String decision) throws Exception {
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/" + decision).param("memberId", "30")
                .contentType("application/json").content("{\"rejectReason\":\"확인 필요\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mapper);
    }
    void token(boolean allowed) {
        Claims claims = mock(Claims.class);
        when(provider.parseToken("test-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("30"); when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("test-jti"); when(claims.get("sid", String.class)).thenReturn("test-session");
        when(claims.getExpiration()).thenReturn(new Date(2000000000000L));
        when(sessions.isAccessAllowed(30L, "USER", "test-session", "test-jti", 2000000000000L)).thenReturn(allowed);
    }
    @Test void invalidSessionCannotReachService() throws Exception {
        token(false);
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/approve").header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized()); verifyNoInteractions(mapper);
    }
    @Test void validJwtOwnerApprovesOwnRequest() throws Exception {
        token(true);
        var group = new SavingGroupPayoutDecisionRow(); group.setGroupId(20L); group.setOwnerMemberId(30L);
        group.setGroupType("친목계"); group.setStatus("01");
        when(mapper.selectPayoutDecisionGroupForUpdate(10L)).thenReturn(group);
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(owner);
        var request = new SavingGroupPayoutDecisionRow(); request.setRequesterId(30L); request.setStatus("PENDING"); request.setHasEvidence(true);
        when(mapper.selectPayoutDecisionRequestForUpdate(20L, 40L, "친목계")).thenReturn(request);
        when(mapper.selectSavingGroupJoinMemberForUpdate(30L)).thenReturn(new SavingGroupJoinMemberRow());
        when(mapper.updatePayoutDecision(20L, 40L, "친목계", "APPROVED", 30L, null)).thenReturn(1);
        mvc.perform(patch("/api/saving-groups/10/payout-requests/40/approve").header("Authorization", "Bearer test-token").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(mapper).updatePayoutDecision(20L, 40L, "친목계", "APPROVED", 30L, null);
    }
}
