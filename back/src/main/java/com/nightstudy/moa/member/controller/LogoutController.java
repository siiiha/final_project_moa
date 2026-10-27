package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.RefreshTokenCookie;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그인 세션과 refresh token 쿠키를 함께 폐기
@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    private final AuthSessionService authSessionService;
    private final RefreshTokenCookie refreshTokenCookie;

    public LogoutController(
            AuthSessionService authSessionService,
            RefreshTokenCookie refreshTokenCookie
    ) {
        this.authSessionService = authSessionService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        refreshTokenCookie.requireTrustedRequest(request);

        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        String accessToken = null;

        if (authorization != null
                && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }

        // 기존 access token 차단과 refresh 세션 폐기를 함께 처리
        authSessionService.logout(
                refreshTokenCookie.read(request),
                accessToken
        );

        refreshTokenCookie.clear(response);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}