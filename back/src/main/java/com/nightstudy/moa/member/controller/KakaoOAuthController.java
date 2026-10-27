package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.OAuthStateManager;
import com.nightstudy.moa.common.auth.SocialLoginResultManager;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

// 카카오 인증과 프론트로의 결과 전달을 처리
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final OAuthStateManager oauthStateManager;
    private final SocialSignupSessionManager signupSessionManager;
    private final SocialLoginResultManager loginResultManager;
    private final String frontendUrl;

    public KakaoOAuthController(
            KakaoOAuthService kakaoOAuthService,
            OAuthStateManager oauthStateManager,
            SocialSignupSessionManager signupSessionManager,
            SocialLoginResultManager loginResultManager,
            @Value("${app.frontend-url:http://localhost:5173}")
            String frontendUrl
    ) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.oauthStateManager = oauthStateManager;
        this.signupSessionManager = signupSessionManager;
        this.loginResultManager = loginResultManager;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        String state = oauthStateManager.issue(request, "kakao");

        URI authorizationUri =
                kakaoOAuthService.createAuthorizationUri(state);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(authorizationUri)
                .header("Cache-Control", "no-store")
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            HttpServletRequest request,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error
    ) {
        // 카카오 API 호출 전에 요청의 유효성을 확인
        oauthStateManager.validateAndConsume(request, "kakao", state);

        if (error != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_AUTH_FAILED",
                    "카카오 로그인이 취소되었거나 승인되지 않았습니다."
            );
        }

        SocialLoginResultDto result = kakaoOAuthService.authenticate(code);

        if (result.isSignupRequired()) {
            String signupToken = signupSessionManager.issue(
                    request,
                    "KAKAO",
                    result.getProviderUserId()
            );

            result.setSignupToken(signupToken);
        }

        // 토큰을 주소에 넣지 않고 서버 세션에 잠시 보관
        loginResultManager.save(request, result);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/member/social/callback"))
                .header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .build();
    }

    // 같은 브라우저의 프론트가 결과를 한 번 받아감
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<SocialLoginResultDto>> result(
            HttpServletRequest request
    ) {
        SocialLoginResultDto result = loginResultManager.consume(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .body(ApiResponse.ok(result));
    }
}