package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.OAuthStateManager;
import com.nightstudy.moa.member.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.web.bind.annotation.RequestParam;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager;

import java.net.URI;

// 카카오 로그인 시작 요청을 처리
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoOAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final OAuthStateManager oauthStateManager;
    private final SocialSignupSessionManager socialSignupSessionManager;

    public KakaoOAuthController(
            KakaoOAuthService kakaoOAuthService,
            OAuthStateManager oauthStateManager,
            SocialSignupSessionManager socialSignupSessionManager
    ) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.oauthStateManager = oauthStateManager;
        this.socialSignupSessionManager = socialSignupSessionManager;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        // 현재 브라우저의 세션에 일회용 검증값을 저장
        String state = oauthStateManager.issue(request, "kakao");

        URI authorizationUri =
                kakaoOAuthService.createAuthorizationUri(state);

        // 브라우저를 카카오 인증 화면으로 이동
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(authorizationUri)
                .header("Cache-Control", "no-store")
                .build();
    }

    // 카카오 인증 결과를 받아 요청 검증과 사용자 확인을 수행
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<SocialLoginResultDto>> callback(
            HttpServletRequest request,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error
    ) {
        // 외부 API 호출 전에 같은 브라우저에서 시작한 요청인지 검증
        oauthStateManager.validateAndConsume(request, "kakao", state);

        if (error != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_AUTH_FAILED",
                    "카카오 로그인이 취소되었거나 승인되지 않았습니다. 다시 시도해주세요."
            );
        }

        // 토큰 발급과 카카오 회원 ID 조회까지 성공하는지 확인
        // 모아 회원 연결과 JWT 발급은 다음 단계에서 추가
        SocialLoginResultDto result = kakaoOAuthService.authenticate(code);

        if (result.isSignupRequired()) {
            String signupToken = socialSignupSessionManager.issue(
                    request,
                    "KAKAO",
                    result.getProviderUserId()
            );

            result.setSignupToken(signupToken);
        }

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Referrer-Policy", "no-referrer")
                .body(ApiResponse.ok(result));
    }
}