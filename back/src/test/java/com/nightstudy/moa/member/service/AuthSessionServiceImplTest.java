package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.service.auth.AuthSessionServiceImpl;
import com.nightstudy.moa.member.service.auth.LogoutService;
import com.nightstudy.moa.member.service.email.EmailVerificationTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.auth.AuthStateCache;
import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.member.mapper.MemberAuthSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// AI작성: Valkey 캐시 우선 인증과 MySQL 대체 조회 규칙을 DB 없이 검증한다.
class AuthSessionServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final String SYSTEM_ROLE = "USER";
    private static final String SESSION_ID =
            "12345678-1234-1234-1234-123456789012";
    private static final String TOKEN_ID = "access-token-id";

    @Mock
    private MemberAuthSessionMapper sessionMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailVerificationTokenProvider tokenProvider;

    @Mock
    private LogoutService logoutService;

    @Mock
    private AuthStateCache authStateCache;

    private AuthSessionServiceImpl authSessionService;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionServiceImpl(
                sessionMapper,
                jwtTokenProvider,
                tokenProvider,
                logoutService,
                authStateCache,
                604_800_000L
        );
    }

    @Test
    // AI작성: 폐기 토큰 캐시에 값이 있으면 MySQL 조회 없이 인증을 거절해야 한다.
    void isAccessAllowedRejectsCachedRevokedToken() {
        when(authStateCache.findRevokedToken(TOKEN_ID))
                .thenReturn(AuthStateCache.CacheLookup.hit(true));

        boolean allowed = authSessionService.isAccessAllowed(
                MEMBER_ID,
                SYSTEM_ROLE,
                SESSION_ID,
                TOKEN_ID,
                futureTime()
        );

        assertThat(allowed).isFalse();
        verify(authStateCache, never()).findSession(anyString());
        verifyNoInteractions(sessionMapper);
    }

    @Test
    // AI작성: JWT 정보와 일치하는 세션 캐시는 MySQL 조회 없이 인증해야 한다.
    void isAccessAllowedAcceptsMatchingCachedSession() {
        long expiresAtMs = futureTime();
        when(authStateCache.findRevokedToken(TOKEN_ID))
                .thenReturn(AuthStateCache.CacheLookup.miss());
        when(authStateCache.findSession(SESSION_ID)).thenReturn(
                AuthStateCache.CacheLookup.hit(
                        new AuthStateCache.SessionState(
                                SESSION_ID,
                                MEMBER_ID,
                                SYSTEM_ROLE,
                                expiresAtMs
                        )
                )
        );

        boolean allowed = authSessionService.isAccessAllowed(
                MEMBER_ID,
                SYSTEM_ROLE,
                SESSION_ID,
                TOKEN_ID,
                expiresAtMs
        );

        assertThat(allowed).isTrue();
        verifyNoInteractions(sessionMapper);
        verify(authStateCache, never()).saveSession(any());
    }

    @Test
    // AI작성: 캐시가 비어 있으면 기존 MySQL 인증 결과로 확인하고 세션 캐시를 복구해야 한다.
    void isAccessAllowedRestoresCacheAfterMiss() {
        long expiresAtMs = futureTime();
        when(authStateCache.findRevokedToken(TOKEN_ID))
                .thenReturn(AuthStateCache.CacheLookup.miss());
        when(authStateCache.findSession(SESSION_ID))
                .thenReturn(AuthStateCache.CacheLookup.miss());
        when(sessionMapper.countActiveSession(
                eq(SESSION_ID),
                eq(MEMBER_ID),
                eq(SYSTEM_ROLE),
                anyLong(),
                eq(TOKEN_ID)
        )).thenReturn(1);

        boolean allowed = authSessionService.isAccessAllowed(
                MEMBER_ID,
                SYSTEM_ROLE,
                SESSION_ID,
                TOKEN_ID,
                expiresAtMs
        );

        assertThat(allowed).isTrue();
        verify(sessionMapper).countActiveSession(
                eq(SESSION_ID),
                eq(MEMBER_ID),
                eq(SYSTEM_ROLE),
                anyLong(),
                eq(TOKEN_ID)
        );
        verify(authStateCache).saveSession(argThat(session ->
                SESSION_ID.equals(session.sessionId())
                        && MEMBER_ID.equals(session.memberId())
                        && SYSTEM_ROLE.equals(session.systemRole())
                        && expiresAtMs == session.expiresAtMs()
        ));
    }

    @Test
    // AI작성: Valkey 장애가 발생하면 캐시를 신뢰하지 않고 MySQL 인증 조회로 대체해야 한다.
    void isAccessAllowedFallsBackToDatabaseWhenCacheUnavailable() {
        long expiresAtMs = futureTime();
        when(authStateCache.findRevokedToken(TOKEN_ID))
                .thenReturn(AuthStateCache.CacheLookup.unavailable());
        when(sessionMapper.countActiveSession(
                eq(SESSION_ID),
                eq(MEMBER_ID),
                eq(SYSTEM_ROLE),
                anyLong(),
                eq(TOKEN_ID)
        )).thenReturn(1);

        boolean allowed = authSessionService.isAccessAllowed(
                MEMBER_ID,
                SYSTEM_ROLE,
                SESSION_ID,
                TOKEN_ID,
                expiresAtMs
        );

        assertThat(allowed).isTrue();
        verify(authStateCache, never()).findSession(anyString());
        verify(sessionMapper).countActiveSession(
                eq(SESSION_ID),
                eq(MEMBER_ID),
                eq(SYSTEM_ROLE),
                anyLong(),
                eq(TOKEN_ID)
        );
    }

    private long futureTime() {
        return System.currentTimeMillis() + 60_000;
    }
}
