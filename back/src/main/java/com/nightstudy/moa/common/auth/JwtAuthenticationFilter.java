package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// JWT 검증 및 로그아웃한 토큰의 재접근 차단
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final RevokedTokenMapper revokedTokenMapper;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter,
            RevokedTokenMapper revokedTokenMapper
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
        this.revokedTokenMapper = revokedTokenMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // 토큰이 없으면 접근 허용 여부를 Spring Security에서 판단
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

            // 회원 정보, 만료 시각, 토큰 고유 번호 확인
            if (memberId <= 0
                    || claims.getExpiration() == null
                    || tokenId == null
                    || tokenId.isBlank()
                    || (!"USER".equals(systemRole)
                    && !"ADMIN".equals(systemRole))) {
                throw new IllegalArgumentException("잘못된 토큰 정보");
            }

            // 로그아웃 기록이 있는 토큰은 인증 처리 전에 차단
            if (revokedTokenMapper.countByTokenId(tokenId) > 0) {
                throw new IllegalArgumentException("로그아웃한 토큰");
            }

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + systemRole
                                    )
                            )
                    );

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // 검증한 정보를 Controller에 전달
            request.setAttribute("memberId", memberId);
            request.setAttribute("systemRole", systemRole);
            request.setAttribute("tokenId", tokenId);
            request.setAttribute(
                    "expiresAtMs",
                    claims.getExpiration().getTime()
            );

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();

            errorResponseWriter.write(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "유효하지 않거나 만료 또는 로그아웃된 토큰입니다."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}