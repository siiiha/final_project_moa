package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.SocialSignupSessionManager;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager.PendingSignup;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import com.nightstudy.moa.member.service.SocialSignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 소셜 인증을 마친 사용자의 추가 가입 요청을 처리
@RestController
@RequestMapping("/api/auth/social")
public class SocialSignupController {

    private final SocialSignupService socialSignupService;
    private final SocialSignupSessionManager signupSessionManager;

    public SocialSignupController(
            SocialSignupService socialSignupService,
            SocialSignupSessionManager signupSessionManager
    ) {
        this.socialSignupService = socialSignupService;
        this.signupSessionManager = signupSessionManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SocialLoginResultDto>> signup(
            HttpServletRequest httpRequest,
            @Valid @RequestBody SocialSignupRequest request
    ) {
        // 브라우저 세션과 가입 토큰이 일치하는지 먼저 검증
        PendingSignup pending =
                signupSessionManager.requirePendingSignup(
                        httpRequest,
                        request.getSignupToken()
                );

        SocialLoginResultDto result =
                socialSignupService.signup(request, pending);

        // 트랜잭션이 정상 완료된 뒤 가입 정보를 제거
        signupSessionManager.clear(httpRequest, pending);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(ApiResponse.ok(result));
    }
}