package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.MemberLoginRequest;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import com.nightstudy.moa.member.dto.MemberProfileUpdateRequest;
import com.nightstudy.moa.member.dto.MemberWithdrawalRequest;
import com.nightstudy.moa.member.dto.MemberPasswordUpdateRequest;

import com.nightstudy.moa.member.dto.PasswordResetRequest;
import com.nightstudy.moa.member.dto.MemberTermDto;
import java.util.List;

public interface MemberService {

    Long signup(MemberSignupRequest request);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);

    String login(MemberLoginRequest request);

    Long signupSocial(SocialSignupRequest request);

    MemberProfileResponse getMyProfile(Long memberId);

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
}