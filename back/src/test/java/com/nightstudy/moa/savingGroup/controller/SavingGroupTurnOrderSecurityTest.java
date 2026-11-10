package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.notification.service.NotificationService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.auth.*;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupServiceImpl;
import io.jsonwebtoken.Claims;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

// AI작성: 실제 JWT 필터→Controller→Service를 연결한다. 토큰 파싱·세션·DB만 대역으로 분리한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupTurnOrderSecurityTest {
    @Mock JwtTokenProvider provider;
    @Mock RevokedTokenMapper revoked;
    @Mock AuthSessionService sessions;
    @Mock SavingGroupMapper mapper;
    MockMvc mvc;
    @BeforeEach void setup() {
        var filter = new JwtAuthenticationFilter(provider, new SecurityErrorResponseWriter(new ObjectMapper()), revoked, sessions);
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(new SavingGroupServiceImpl(mapper, null, null, org.mockito.Mockito.mock(NotificationService.class), org.mockito.Mockito.mock(com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassifierClient.class))))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(filter).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    // AI작성: 모의 응답 DTO가 아니라 실제 상세 Service→Controller 응답에 유형이 전달되는지 회귀 검증한다.
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"순번계", "친목계"})
    void detailResponseIncludesStoredTypeForRecommendationButton(String type) throws Exception {
        token(true);
        var group = new SavingGroupRow(); group.setSavingGroupId(10L); group.setGroupId(20L);
        group.setGroupType(type); group.setGroupName("테스트 모임"); group.setStatus("01");
        when(mapper.selectSavingGroupDetail(20L)).thenReturn(group);
        when(mapper.selectApprovedMemberRoles(20L, 30L)).thenReturn(List.of("OWNER"));
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(owner);
        mvc.perform(get("/api/saving-groups/20").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.groupType").value(type))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.membershipStatus").value("APPROVED"));
    }
    void token(boolean allowed) {
        Claims claims = mock(Claims.class);
        when(provider.parseToken("test-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("30"); when(claims.get("role", String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("test-jti"); when(claims.get("sid", String.class)).thenReturn("test-session");
        when(claims.getExpiration()).thenReturn(new Date(2000000000000L));
        when(sessions.isAccessAllowed(30L, "USER", "test-session", "test-jti", 2000000000000L)).thenReturn(allowed);
    }
    @Test void anonymousCannotSpoofIdentity() throws Exception {
        mvc.perform(get("/api/saving-groups/10/turn-orders/recommendations").param("memberId", "30"))
                .andExpect(status().isUnauthorized()); verifyNoInteractions(mapper);
    }
    @Test void invalidSessionCannotReachService() throws Exception {
        token(false);
        mvc.perform(get("/api/saving-groups/10/turn-orders/recommendations").header("Authorization", "Bearer test-token"))
                .andExpect(status().isUnauthorized()); verifyNoInteractions(mapper);
    }
    @Test void validJwtOwnerReceivesSimulationWithoutWrites() throws Exception {
        token(true);
        var group = new SavingGroupTurnOrderContextRow(); group.setGroupId(20L); group.setGroupType("순번계");
        group.setStatus("02"); group.setDuesAmount(new BigDecimal("10000"));
        when(mapper.selectTurnOrderRecommendationContext(10L)).thenReturn(group);
        var owner = new SavingGroupMembershipRelationRow(); owner.setMemberRole("OWNER"); owner.setMembershipStatus("APPROVED");
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(owner); when(mapper.existsMember(30L)).thenReturn(true);
        var row = new SavingGroupTurnOrderMemberRow(); row.setMemberId("30"); row.setNickname("테스트계주");
        when(mapper.selectTurnOrderRecommendationMembers(20L)).thenReturn(List.of(row));
        mvc.perform(get("/api/saving-groups/10/turn-orders/recommendations").header("Authorization", "Bearer test-token").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].memberId").value("30"))
                .andExpect(jsonPath("$.data[0].expectedPayoutAmount").value(10000));
        verify(mapper).selectTurnOrderRecommendationContext(10L); verify(mapper).selectSavingGroupMembership(20L, 30L);
        verify(mapper).existsMember(30L); verify(mapper).selectTurnOrderRecommendationMembers(20L); verifyNoMoreInteractions(mapper);
    }
}
