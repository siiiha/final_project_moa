package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.service.LogoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 로그아웃 요청을 받아 검증된 토큰을 사용 중지 처리
@RestController
@RequestMapping("/api/auth")
public class LogoutController {

    private final LogoutService logoutService;

    public LogoutController(LogoutService logoutService) {
        this.logoutService = logoutService;
    }

    // JWT 필터에서 검증하고 전달한 토큰 정보 사용
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute("tokenId") String tokenId,
            @RequestAttribute("expiresAtMs") Long expiresAtMs
    ) {
        logoutService.logout(tokenId, expiresAtMs);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}