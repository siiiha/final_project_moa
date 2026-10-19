package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import com.nightstudy.moa.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nightstudy.moa.member.dto.MemberProfileUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/members")
public class MemberProfileController {

    private final MemberService memberService;

    public MemberProfileController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>>
    getMyProfile(
            @RequestAttribute("memberId") Long memberId
    ) {
        MemberProfileResponse profile = memberService.getMyProfile(memberId);

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    // 로그인한 회원의 비밀번호를 확인하고 회원정보 수정
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MemberProfileResponse>>
    updateMyProfile(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody MemberProfileUpdateRequest
                    request
    ) {
        MemberProfileResponse profile =
                memberService.updateMyProfile(memberId, request);

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
