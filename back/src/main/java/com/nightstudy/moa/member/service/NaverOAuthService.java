package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.SocialLoginResultDto;

import java.net.URI;

// 네이버 인증 주소 생성과 인증 처리를 정의
public interface NaverOAuthService {

    URI createAuthorizationUri(String state);

    SocialLoginResultDto authenticate(String code, String state);
}