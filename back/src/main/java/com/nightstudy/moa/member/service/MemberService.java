package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.*;

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
}