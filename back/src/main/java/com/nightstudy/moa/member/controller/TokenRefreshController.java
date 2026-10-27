package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.RefreshTokenCookie;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberAuthTokenDto;
import com.nightstudy.moa.member.service.AuthSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// refresh token을 사용한 access token 재발급을 처리
@RestController
@RequestMapping("/api/auth")
public class TokenRefreshController {

    private final AuthSessionService authSessionService;
    private final RefreshTokenCookie refreshTokenCookie;

    public TokenRefreshController(
            AuthSessionService authSessionService,
            RefreshTokenCookie refreshTokenCookie
    ) {
        this.authSessionService = authSessionService;
        this.refreshTokenCookie = refreshTokenCookie;
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 쿠키를 사용하기 전에 요청 출처와 전용 헤더를 확인
        refreshTokenCookie.requireTrustedRequest(request);

        try {
            MemberAuthTokenDto tokens = authSessionService.refresh(
                    refreshTokenCookie.read(request)
            );

            refreshTokenCookie.write(
                    response,
                    tokens.getRefreshToken(),
                    tokens.getExpiresAtMs()
            );

            return ResponseEntity.ok(
                    ApiResponse.ok(tokens.getAccessToken())
            );
        } catch (ApiException e) {
            // 사용할 수 없는 인증 쿠키를 브라우저에서도 제거
            if (e.getStatus() == HttpStatus.UNAUTHORIZED
                    || e.getStatus() == HttpStatus.FORBIDDEN) {
                refreshTokenCookie.clear(response);
            }

            throw e;
        }
    }
}