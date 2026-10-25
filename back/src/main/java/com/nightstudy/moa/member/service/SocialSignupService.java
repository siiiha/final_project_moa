package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.auth.SocialSignupSessionManager.PendingSignup;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.dto.SocialSignupRequest;

// 인증된 소셜 계정으로 모아 회원가입을 완료
public interface SocialSignupService {

    SocialLoginResultDto signup(
            SocialSignupRequest request,
            PendingSignup pending
    );
}