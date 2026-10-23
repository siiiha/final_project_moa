package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.*;
import com.nightstudy.moa.member.dto.MemberPasswordUpdateRequest;

public interface MemberService {

    Long signup(MemberSignupRequest request);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);

    String login(MemberLoginRequest request);

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
}