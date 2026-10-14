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


@RestController
@RequestMapping("/api/auth")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
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
}