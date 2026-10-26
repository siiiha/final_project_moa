package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.auth.LoginSessionManager;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager.PendingSignup;
import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 소셜 가입 완료 시 공통 로그인 세션을 발급
@RestController
@RequestMapping("/api/auth/social")
public class SocialSignupController {

    private final SocialSignupSessionManager signupSessionManager;
    private final LoginSessionManager loginSessionManager;

    public SocialSignupController(
            SocialSignupSessionManager signupSessionManager,
            LoginSessionManager loginSessionManager
    ) {
        this.signupSessionManager = signupSessionManager;
        this.loginSessionManager = loginSessionManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SocialLoginResultDto>> signup(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @Valid @RequestBody SocialSignupRequest request
    ) {
        PendingSignup pending =
                signupSessionManager.requirePendingSignup(
                        httpRequest,
                        request.getSignupToken()
                );

        SocialLoginResultDto result =
                loginSessionManager.completeSignup(
                        request,
                        pending,
                        httpResponse
                );

        // 가입과 세션 발급이 커밋된 뒤 임시 가입 정보를 제거
        signupSessionManager.clear(httpRequest, pending);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Cache-Control", "no-store")
                .body(ApiResponse.ok(result));
    }
}