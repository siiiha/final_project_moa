package com.nightstudy.moa.common.auth;

import com.nightstudy.moa.common.exception.ApiException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

// refresh token 쿠키와 재발급·로그아웃 요청의 출처를 관리
@Component
public class RefreshTokenCookie {

    public static final String NAME = "moa-refresh";

    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final String frontendOrigin;

    public RefreshTokenCookie(
            @Value("${jwt.refresh-cookie-secure:false}") boolean secure,
            @Value("${app.frontend-url:http://localhost:5173}")
            String frontendUrl
    ) {
        URI uri = URI.create(frontendUrl);

        if (uri.getHost() == null
                || uri.getUserInfo() != null
                || (!"http".equals(uri.getScheme())
                && !"https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException(
                    "프론트 주소는 올바른 HTTP 또는 HTTPS 주소여야 합니다."
            );
        }

        this.secure = secure;
        this.frontendOrigin =
                uri.getScheme() + "://" + uri.getRawAuthority();
    }

    // 브라우저가 전달한 쿠키에서 refresh token을 읽음
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        String token = null;

        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName())) {
                // AI작성: 같은 이름의 쿠키가 중복되면 사용하지 않는다.
                if (token != null) {
                    return null;
                }

                token = cookie.getValue();
            }
        }

        return token;
    }

    // JavaScript가 읽을 수 없는 HttpOnly 쿠키로만 전달
    public void write(
            HttpServletResponse response,
            String refreshToken,
            long expiresAtMs
    ) {
        long remainingMs = Math.max(
                0,
                expiresAtMs - System.currentTimeMillis()
        );

        ResponseCookie cookie = ResponseCookie.from(NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(Duration.ofMillis(remainingMs))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    // 발급할 때와 같은 경로로 쿠키를 삭제
    public void clear(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(PATH)
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    // 쿠키를 사용하는 요청은 전용 헤더와 요청 출처를 확인
    public void requireTrustedRequest(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        String fetchSite = request.getHeader("Sec-Fetch-Site");

        if (!"1".equals(request.getHeader("X-Moa-Request"))
                || "cross-site".equalsIgnoreCase(fetchSite)
                || (origin != null && !frontendOrigin.equals(origin))) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "UNTRUSTED_AUTH_REQUEST",
                    "허용되지 않은 인증 요청입니다."
            );
        }
    }
}