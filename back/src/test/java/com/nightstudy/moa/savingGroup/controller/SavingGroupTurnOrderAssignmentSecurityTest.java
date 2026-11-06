package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.auth.*;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.auth.AuthSessionService;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupTurnOrderServiceImpl;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

// AI작성: 실제 JWT 필터와 Service를 연결해 PUT 요청의 위조·만료 세션·타 모임 권한을 차단한다.
class SavingGroupTurnOrderAssignmentSecurityTest {
    JwtTokenProvider provider; AuthSessionService sessions; SavingGroupMapper mapper; MockMvc mvc;
    @BeforeEach void setup() {
        provider=mock(JwtTokenProvider.class); sessions=mock(AuthSessionService.class); mapper=mock(SavingGroupMapper.class);
        var filter=new JwtAuthenticationFilter(provider,new SecurityErrorResponseWriter(new ObjectMapper()),mock(RevokedTokenMapper.class),sessions);
        mvc=MockMvcBuilders.standaloneSetup(new SavingGroupTurnOrderController(new SavingGroupTurnOrderServiceImpl(mapper)))
            .setControllerAdvice(new GlobalExceptionHandler()).addFilters(filter).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void token(boolean allowed) {
        var claims=mock(Claims.class); when(provider.parseToken("test-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("30"); when(claims.get("role",String.class)).thenReturn("USER");
        when(claims.getId()).thenReturn("test-jti"); when(claims.get("sid",String.class)).thenReturn("test-session");
        when(claims.getExpiration()).thenReturn(new Date(2000000000000L));
        when(sessions.isAccessAllowed(30L,"USER","test-session","test-jti",2000000000000L)).thenReturn(allowed);
    }
    @Test void anonymousCannotSpoofOwner() throws Exception {
        mvc.perform(put("/api/saving-groups/10/turn-orders").param("memberId","30").contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnOrders\":[{\"memberId\":30,\"sequenceNumber\":1}]}"))
            .andExpect(status().isUnauthorized()); verifyNoInteractions(mapper);
    }
    @Test void invalidSessionCannotWrite() throws Exception {
        token(false);
        mvc.perform(put("/api/saving-groups/10/turn-orders").header("Authorization","Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"turnOrders\":[{\"memberId\":30,\"sequenceNumber\":1}]}"))
            .andExpect(status().isUnauthorized()); verifyNoInteractions(mapper);
    }
    @Test void validJwtStillRequiresOwnershipOfTargetGroup() throws Exception {
        token(true);
        var group=new com.nightstudy.moa.savingGroup.dto.SavingGroupTurnOrderPlanRow(); group.setGroupId(20L);
        when(mapper.selectTurnOrderGroupForUpdate(10L)).thenReturn(group);
        mvc.perform(put("/api/saving-groups/10/turn-orders").header("Authorization","Bearer test-token")
            .param("memberId","99").contentType(MediaType.APPLICATION_JSON)
            .content("{\"turnOrders\":[{\"memberId\":30,\"sequenceNumber\":1}]}"))
            .andExpect(status().isForbidden());
        verify(mapper).selectSavingGroupMembershipForUpdate(20L,30L);
        verify(mapper,never()).insertTurnOrderPlan(any());
    }
}
