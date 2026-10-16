package com.nightstudy.moa.common.auth;

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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
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

        // 토큰이 없으면 다음 단계에서 접근 허용 여부 판단
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

            if (memberId <= 0
                    || claims.getExpiration() == null
                    || (!"USER".equals(systemRole)
                    && !"ADMIN".equals(systemRole))) {
                throw new IllegalArgumentException("잘못된 토큰 정보");
            }

            // Spring Security에 인증된 회원 정보 등록
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

            // 기존 Controller에서 사용하던 정보 유지
            request.setAttribute("memberId", memberId);
            request.setAttribute("systemRole", systemRole);

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();

            errorResponseWriter.write(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "유효하지 않거나 만료된 토큰입니다."
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}