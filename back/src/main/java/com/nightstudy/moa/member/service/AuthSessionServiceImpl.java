package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberAuthSessionDto;
import com.nightstudy.moa.member.dto.MemberAuthTokenDto;
import com.nightstudy.moa.member.dto.MemberRefreshTokenDto;
import com.nightstudy.moa.member.dto.SocialLoginMemberDto;
import com.nightstudy.moa.member.exception.AuthSessionRevokedException;
import com.nightstudy.moa.member.mapper.MemberAuthSessionMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

// 기존 인증 코드를 재사용해 로그인 세션과 refresh token을 관리
@Service
public class AuthSessionServiceImpl implements AuthSessionService {

    private final MemberAuthSessionMapper sessionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationTokenProvider tokenProvider;
    private final LogoutService logoutService;
    private final long refreshExpirationMs;

    public AuthSessionServiceImpl(
            MemberAuthSessionMapper sessionMapper,
            JwtTokenProvider jwtTokenProvider,
            EmailVerificationTokenProvider tokenProvider,
            LogoutService logoutService,
            @Value("${jwt.refresh-expiration-ms:604800000}")
            long refreshExpirationMs
    ) {
        if (refreshExpirationMs <= 0
                || refreshExpirationMs > 30L * 24 * 60 * 60 * 1000) {
            throw new IllegalArgumentException(
                    "refresh token 유효기간은 0보다 크고 30일 이하여야 합니다."
            );
        }

        this.sessionMapper = sessionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenProvider = tokenProvider;
        this.logoutService = logoutService;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // 로그인 검증을 마친 회원에게 새 세션을 발급
    @Override
    @Transactional
    public MemberAuthTokenDto startSession(String accessToken) {
        Claims claims = jwtTokenProvider.parseToken(accessToken);
        Long memberId = Long.valueOf(claims.getSubject());

        SocialLoginMemberDto member =
                sessionMapper.findMemberById(memberId);

        if (!isActiveMember(member)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "로그인할 수 없는 계정 상태입니다."
            );
        }

        MemberAuthSessionDto session = new MemberAuthSessionDto();
        session.setSessionId(UUID.randomUUID().toString());
        session.setMemberId(member.getMemberId());
        session.setExpiresAtMs(
                System.currentTimeMillis() + refreshExpirationMs
        );

        if (sessionMapper.insertSession(session) != 1) {
            throw new IllegalStateException("로그인 세션 저장에 실패했습니다.");
        }

        return issueTokens(session, member);
    }

    // 세션 폐기 예외가 발생해도 폐기 기록은 DB에 유지
    @Override
    @Transactional(noRollbackFor = AuthSessionRevokedException.class)
    public MemberAuthTokenDto refresh(String refreshToken) {
        if (!isValidRefreshToken(refreshToken)) {
            throw invalidRefreshToken();
        }

        String tokenHash = tokenProvider.hashToken(refreshToken);
        String sessionId =
                sessionMapper.findSessionIdByTokenHash(tokenHash);

        if (sessionId == null) {
            throw invalidRefreshToken();
        }

        // 항상 세션을 먼저 잠그고 토큰을 잠가 동시 요청을 제어
        MemberAuthSessionDto session =
                sessionMapper.findSessionForUpdate(sessionId);

        if (session == null
                || session.getRevokedAt() != null
                || session.getExpiresAtMs() == null
                || session.getExpiresAtMs() <= System.currentTimeMillis()) {
            throw invalidRefreshToken();
        }

        MemberRefreshTokenDto token =
                sessionMapper.findTokenForUpdate(tokenHash);

        if (token == null || !sessionId.equals(token.getSessionId())) {
            throw invalidRefreshToken();
        }

        // 이미 교체된 토큰이 다시 들어오면 해당 세션 전체를 폐기
        if (token.getUsedAt() != null) {
            throw revokeForReuse(sessionId);
        }

        SocialLoginMemberDto member =
                sessionMapper.findMemberById(session.getMemberId());

        if (!isActiveMember(member)) {
            sessionMapper.revokeSession(sessionId);

            throw new AuthSessionRevokedException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "로그인할 수 없는 계정 상태입니다."
            );
        }

        if (sessionMapper.markTokenUsed(tokenHash) != 1) {
            throw revokeForReuse(sessionId);
        }

        // 토큰을 교체해도 최초 로그인 세션의 만료 시각은 연장하지 않음
        return issueTokens(session, member);
    }

    // access token이 만료됐어도 refresh token 세션은 로그아웃할 수 있음
    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        Set<String> sessionIds = new TreeSet<>();

        if (isValidRefreshToken(refreshToken)) {
            String sessionId = sessionMapper.findSessionIdByTokenHash(
                    tokenProvider.hashToken(refreshToken)
            );

            if (sessionId != null) {
                sessionIds.add(sessionId);
            }
        }

        Claims claims = readLogoutClaims(accessToken);

        if (claims != null) {
            String sessionId = claims.get("sid", String.class);

            if (sessionId != null) {
                sessionIds.add(sessionId);
            }
        }

        // 서로 다른 세션이 전달돼도 항상 같은 순서로 잠금
        for (String sessionId : sessionIds) {
            MemberAuthSessionDto session =
                    sessionMapper.findSessionForUpdate(sessionId);

            if (session != null) {
                sessionMapper.revokeSession(sessionId);
            }
        }

        // 기존 access token 로그아웃 기록도 계속 사용
        if (claims != null
                && claims.getId() != null
                && !claims.getId().isBlank()
                && claims.getExpiration() != null
                && claims.getExpiration().getTime()
                > System.currentTimeMillis()) {
            logoutService.logout(
                    claims.getId(),
                    claims.getExpiration().getTime()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccessAllowed(
            Long memberId,
            String systemRole,
            String sessionId
    ) {
        if (memberId == null || memberId <= 0
                || !isValidRole(systemRole)) {
            return false;
        }

        // 기존 세션 번호 없는 JWT도 현재 회원 상태를 검사
        if (sessionId == null) {
            SocialLoginMemberDto member =
                    sessionMapper.findMemberById(memberId);

            return isActiveMember(member)
                    && systemRole.equals(member.getSystemRole());
        }

        if (sessionId.isBlank() || sessionId.length() != 36) {
            return false;
        }

        return sessionMapper.countActiveSession(
                sessionId,
                memberId,
                systemRole,
                System.currentTimeMillis()
        ) == 1;
    }

    // 난수 생성·SHA-256 해시 변환은 기존 유틸리티를 재사용
    private MemberAuthTokenDto issueTokens(
            MemberAuthSessionDto session,
            SocialLoginMemberDto member
    ) {
        String refreshToken = tokenProvider.createToken();

        MemberRefreshTokenDto token = new MemberRefreshTokenDto();
        token.setTokenHash(tokenProvider.hashToken(refreshToken));
        token.setSessionId(session.getSessionId());

        if (sessionMapper.insertToken(token) != 1) {
            throw new IllegalStateException("refresh token 저장에 실패했습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(
                member.getMemberId(),
                member.getSystemRole(),
                session.getSessionId()
        );

        return new MemberAuthTokenDto(
                accessToken,
                refreshToken,
                session.getExpiresAtMs()
        );
    }

    private boolean isActiveMember(SocialLoginMemberDto member) {
        return member != null
                && member.getMemberId() != null
                && member.getMemberId() > 0
                && member.getDeletedAt() == null
                && "ACTIVE".equals(member.getAccountStatus())
                && isValidRole(member.getSystemRole());
    }

    private boolean isValidRole(String systemRole) {
        return "USER".equals(systemRole) || "ADMIN".equals(systemRole);
    }

    private boolean isValidRefreshToken(String token) {
        return token != null && token.matches("^[A-Za-z0-9_-]{43}$");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_REFRESH_TOKEN",
                "로그인 정보가 만료되었거나 유효하지 않습니다. 다시 로그인해주세요."
        );
    }

    private AuthSessionRevokedException revokeForReuse(String sessionId) {
        sessionMapper.revokeSession(sessionId);

        return new AuthSessionRevokedException(
                HttpStatus.UNAUTHORIZED,
                "REFRESH_TOKEN_REUSED",
                "로그인 정보의 재사용이 감지되었습니다. 다시 로그인해주세요."
        );
    }

    private Claims readLogoutClaims(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }

        try {
            return jwtTokenProvider.parseToken(accessToken);
        } catch (ExpiredJwtException e) {
            // 서명 검증을 통과한 만료 JWT는 세션 폐기 목적으로만 사용
            return e.getClaims();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}