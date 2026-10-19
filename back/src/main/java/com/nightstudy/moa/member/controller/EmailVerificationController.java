package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.EmailCheckRequest;
import com.nightstudy.moa.member.dto.EmailVerificationRequest;
import com.nightstudy.moa.member.service.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(
            EmailVerificationService emailVerificationService
    ) {
        this.emailVerificationService = emailVerificationService;
    }

    // 인증번호 요청
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> sendCode(
            @Valid @RequestBody EmailCheckRequest request
    ) {
        Long verificationRequestId =
                emailVerificationService.sendVerificationCode(
                        request.getEmail()
                );

        return ResponseEntity.ok(
                ApiResponse.ok(verificationRequestId)
        );
    }

    // 인증번호 검증
    // 인증 성공 시 회원가입에 사용할 토큰을 응답으로 전달
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        String token = emailVerificationService.verifyCode(
                request.getVerificationRequestId(),
                request.getCode()
        );

        return ResponseEntity.ok(ApiResponse.ok(token));
    }
}
