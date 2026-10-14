package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public LoginInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        // 브라우저가 보내는 사전 확인 요청은 통과
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "LOGIN_REQUIRED",
                    "로그인이 필요합니다."
            );
        }

        String token = authorization.substring(7);

        try {
            Claims claims = jwtTokenProvider.parseToken(token);
            Long memberId = Long.valueOf(claims.getSubject());

            request.setAttribute("memberId", memberId);
            request.setAttribute("systemRole", claims.get("role", String.class));

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "유효하지 않거나 만료된 토큰입니다."
            );
        }
    }
}