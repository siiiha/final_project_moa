package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.SocialLoginResultDto;

import java.net.URI;

public interface KakaoOAuthService {

    URI createAuthorizationUri(String state);

    // 카카오 인증 후 기존 회원 로그인 또는 신규 가입 필요 여부 반환
    SocialLoginResultDto authenticate(String code);
}