package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.MemberLoginRequest;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import com.nightstudy.moa.member.dto.PasswordResetRequest;

public interface MemberService {

    Long signup(MemberSignupRequest request);

    boolean isEmailAvailable(String email);

    boolean isNicknameAvailable(String nickname);

    String login(MemberLoginRequest request);

    Long signupSocial(SocialSignupRequest request);

    void resetPassword(PasswordResetRequest request);
}