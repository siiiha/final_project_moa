package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nightstudy.moa.member.dto.EmailCheckRequest;
import com.nightstudy.moa.member.dto.NicknameCheckRequest;
import com.nightstudy.moa.member.dto.MemberLoginRequest;
import com.nightstudy.moa.common.auth.LoginSessionManager;
import jakarta.servlet.http.HttpServletResponse;
import com.nightstudy.moa.member.dto.PasswordResetRequest;


@RestController
@RequestMapping("/api/auth")
public class MemberController {

    private final MemberService memberService;
    private final LoginSessionManager loginSessionManager;

    public MemberController(
            MemberService memberService,
            LoginSessionManager loginSessionManager
    ) {
        this.memberService = memberService;
        this.loginSessionManager = loginSessionManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(
            @Valid @RequestBody MemberSignupRequest request
    ) {
        Long memberId = memberService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(memberId));
    }

    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(
            @Valid @RequestBody EmailCheckRequest request
    ) {
        boolean available =
                memberService.isEmailAvailable(request.getEmail());

        return ResponseEntity.ok(ApiResponse.ok(available));
    }

    @PostMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(
            @Valid @RequestBody NicknameCheckRequest request
    ) {
        boolean available =
                memberService.isNicknameAvailable(request.getNickname());

        return ResponseEntity.ok(ApiResponse.ok(available));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(
            @Valid @RequestBody MemberLoginRequest request,
            HttpServletResponse response
    ) {
        String verifiedToken = memberService.login(request);

        String accessToken = loginSessionManager.completeLogin(
                verifiedToken,
                response
        );

        return ResponseEntity.ok(ApiResponse.ok(accessToken));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Long>> me(
            @RequestAttribute("memberId") Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(memberId));
    }

    // 이메일 인증을 완료한 사용자의 비밀번호를 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        memberService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}