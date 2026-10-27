package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.config.NaverOAuthProperties;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// 네이버 인증 후 공통 모아 로그인 처리로 연결
@Service
public class NaverOAuthServiceImpl implements NaverOAuthService {

    private final NaverOAuthProperties properties;
    private final NaverApiClient naverApiClient;
    private final SocialAccountLoginService socialAccountLoginService;

    public NaverOAuthServiceImpl(
            NaverOAuthProperties properties,
            NaverApiClient naverApiClient,
            SocialAccountLoginService socialAccountLoginService
    ) {
        this.properties = properties;
        this.naverApiClient = naverApiClient;
        this.socialAccountLoginService = socialAccountLoginService;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        return UriComponentsBuilder
                .fromUriString("https://nid.naver.com/oauth2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                // AI작성: 네이버에 로그인돼 있어도 사용자 인증을 다시 요청한다.
                .queryParam("auth_type", "reauthenticate")
                .build()
                .encode()
                .toUri();
    }

    @Override
    public SocialLoginResultDto authenticate(String code, String state) {
        // Controller에서 state 검증 후 전달한 인가 코드로 토큰을 받음
        String accessToken =
                naverApiClient.exchangeCodeForAccessToken(code, state);

        String naverUserId = naverApiClient.getUserId(accessToken);

        // 제공사와 검증된 사용자 ID로 모아 회원을 조회
        return socialAccountLoginService.login("NAVER", naverUserId);
    }
}