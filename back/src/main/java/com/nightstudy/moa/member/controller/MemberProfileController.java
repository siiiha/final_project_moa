package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import com.nightstudy.moa.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nightstudy.moa.member.dto.MemberProfileUpdateRequest;
import jakarta.validation.Valid;
import com.nightstudy.moa.member.dto.MemberPasswordUpdateRequest;
import com.nightstudy.moa.member.dto.MemberWithdrawalRequest;
import org.springframework.web.bind.annotation.DeleteMapping;

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

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody MemberPasswordUpdateRequest request
    ) {
        memberService.updatePassword(memberId, request);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdrawMember(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody MemberWithdrawalRequest request
    ) {
        memberService.withdrawMember(memberId, request);

        return ResponseEntity.ok(ApiResponse.<Void>ok(null));
    }
}
