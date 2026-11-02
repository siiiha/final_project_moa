package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.auth.MemberLoginRequest;
import com.nightstudy.moa.member.dto.auth.PasswordResetRequest;
import com.nightstudy.moa.member.dto.identity.IdentityVerificationRequest;
import com.nightstudy.moa.member.dto.identity.IdentityVerificationStartResponse;
import com.nightstudy.moa.member.dto.profile.MemberPasswordUpdateRequest;
import com.nightstudy.moa.member.dto.profile.MemberProfileResponse;
import com.nightstudy.moa.member.dto.profile.MemberProfileUpdateRequest;
import com.nightstudy.moa.member.dto.region.MemberRegionDto;
import com.nightstudy.moa.member.dto.region.MemberRegionUpdateRequest;
import com.nightstudy.moa.member.dto.region.RegionVerificationRequest;
import com.nightstudy.moa.member.dto.signup.MemberSignupRequest;
import com.nightstudy.moa.member.dto.social.SocialSignupRequest;

import com.nightstudy.moa.member.dto.terms.MemberTermDto;
import com.nightstudy.moa.member.dto.withdrawal.MemberWithdrawalRequest;

import java.util.List;

public interface MemberService {

    Long signup(MemberSignupRequest request);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);

    String login(MemberLoginRequest request);

    Long signupSocial(SocialSignupRequest request);

    MemberProfileResponse getMyProfile(Long memberId);
    MemberProfileResponse verifyRegion(
            Long memberId,
            RegionVerificationRequest request
    );

    MemberProfileResponse updateMyRegion(
            Long memberId,
            MemberRegionUpdateRequest request
    );

    MemberProfileResponse updateMyProfile(
            Long memberId,
            MemberProfileUpdateRequest request
    );

    void updatePassword(
            Long memberId,
            MemberPasswordUpdateRequest request
    );

    void withdrawMember(
            Long memberId,
            MemberWithdrawalRequest request
    );

    void resetPassword(PasswordResetRequest request);

    List<MemberTermDto> getTerms();
    List<MemberRegionDto> getRegions();

    IdentityVerificationStartResponse startIdentityVerification(
            Long memberId
    );

    MemberProfileResponse completeIdentityVerification(
            Long memberId,
            IdentityVerificationRequest request
    );
}