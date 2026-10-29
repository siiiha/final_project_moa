package com.nightstudy.moa.member.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.member.dto.*;
import com.nightstudy.moa.member.service.MemberService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.nightstudy.moa.member.dto.RegionVerificationRequest;

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

    // 로그인한 회원의 지역 인증 처리
    @PostMapping("/me/region-verification")
    public ResponseEntity<ApiResponse<MemberProfileResponse>> verifyRegion(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody RegionVerificationRequest request
    ) {
        MemberProfileResponse profile =
                memberService.verifyRegion(memberId, request);

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
