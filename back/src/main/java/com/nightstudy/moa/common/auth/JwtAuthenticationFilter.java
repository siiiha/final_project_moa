package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import com.nightstudy.moa.member.service.AuthSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 검증과 현재 로그인 세션의 유효성을 함께 확인
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final RevokedTokenMapper revokedTokenMapper;
    private final AuthSessionService authSessionService;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter,
            RevokedTokenMapper revokedTokenMapper,
            AuthSessionService authSessionService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
        this.revokedTokenMapper = revokedTokenMapper;
        this.authSessionService = authSessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        // AI작성: 이 두 API는 Controller에서 쿠키와 요청 출처를 검증한다.
        return "POST".equals(request.getMethod())
                && ("/api/auth/refresh".equals(path)
                || "/api/auth/logout".equals(path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException("잘못된 인증 헤더");
            }

            String token = authorization.substring(7);
            Claims claims = jwtTokenProvider.parseToken(token);

            Long memberId = Long.valueOf(claims.getSubject());
            String systemRole = claims.get("role", String.class);
            String tokenId = claims.getId();
            String sessionId = claims.get("sid", String.class);

            if (memberId <= 0
                    || claims.getExpiration() == null
                    || tokenId == null
                    || tokenId.isBlank()
                    || (!"USER".equals(systemRole)
                    && !"ADMIN".equals(systemRole))) {
                throw new IllegalArgumentException("잘못된 토큰 정보");
            }

            // 기존 로그아웃 토큰 차단 기록을 재사용
            if (revokedTokenMapper.countByTokenId(tokenId) > 0) {
                throw new IllegalArgumentException("로그아웃한 토큰");
            }

            // 탈퇴·정지 회원과 폐기·만료된 로그인 세션을 차단
            if (!authSessionService.isAccessAllowed(
                    memberId,
                    systemRole,
                    sessionId
            )) {
                throw new IllegalArgumentException("유효하지 않은 로그인 세션");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + systemRole
                                    )
                            )
                    );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            request.setAttribute("memberId", memberId);
            request.setAttribute("systemRole", systemRole);
            request.setAttribute("tokenId", tokenId);
            request.setAttribute(
                    "expiresAtMs",
                    claims.getExpiration().getTime()
            );
        } catch (ExpiredJwtException e) {
            // 프론트가 토큰 만료와 다른 인증 실패를 구분
            writeUnauthorized(
                    response,
                    "ACCESS_TOKEN_EXPIRED",
                    "로그인 토큰이 만료되었습니다."
            );
            return;
        } catch (JwtException | IllegalArgumentException e) {
            writeUnauthorized(
                    response,
                    "INVALID_TOKEN",
                    "유효하지 않거나 로그아웃된 로그인 정보입니다."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {
        SecurityContextHolder.clearContext();

        errorResponseWriter.write(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                code,
                message
        );
    }
}