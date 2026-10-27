package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.SocialLoginResultDto;

// 제공사에서 인증한 계정으로 모아 로그인을 처리
public interface SocialAccountLoginService {

    SocialLoginResultDto login(
            String provider,
            String providerUserId
    );
}