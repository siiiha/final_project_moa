package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.config.KakaoOAuthProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.SocialLoginMemberDto;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.mapper.MemberSocialAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// 카카오 인증과 연결된 모아 회원의 로그인을 처리
@Service
public class KakaoOAuthServiceImpl implements KakaoOAuthService {

    private final KakaoOAuthProperties properties;
    private final KakaoApiClient kakaoApiClient;
    private final MemberSocialAccountMapper socialAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public KakaoOAuthServiceImpl(
            KakaoOAuthProperties properties,
            KakaoApiClient kakaoApiClient,
            MemberSocialAccountMapper socialAccountMapper,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.properties = properties;
        this.kakaoApiClient = kakaoApiClient;
        this.socialAccountMapper = socialAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        return UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }

    @Override
    public SocialLoginResultDto authenticate(String code) {
        // 카카오 서버에서 확인한 사용자 ID만 회원 조회에 사용
        String accessToken =
                kakaoApiClient.exchangeCodeForAccessToken(code);
        Long kakaoUserId = kakaoApiClient.getUserId(accessToken);

        SocialLoginMemberDto member =
                socialAccountMapper.findByProviderAndProviderUserId(
                        "KAKAO",
                        kakaoUserId.toString()
                );

        SocialLoginResultDto result = new SocialLoginResultDto();

        // 연결된 회원이 없으면 추가 가입이 필요
        // 이메일이 같아도 기존 회원에게 자동 연결하지 않음
        if (member == null) {
            result.setSignupRequired(true);
            result.setProviderUserId(kakaoUserId.toString());
            return result;
        }

        // 탈퇴하거나 정지된 회원에게는 JWT를 발급하지 않음
        if (member.getDeletedAt() != null
                || !"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "로그인할 수 없는 계정 상태입니다."
            );
        }

        // 회원 번호와 권한이 올바른 경우에만 토큰을 발급
        if (member.getMemberId() == null
                || member.getMemberId() <= 0
                || (!"USER".equals(member.getSystemRole())
                && !"ADMIN".equals(member.getSystemRole()))) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_MEMBER_AUTH_INFO",
                    "회원 인증 정보를 확인할 수 없습니다."
            );
        }

        result.setSignupRequired(false);
        result.setToken(jwtTokenProvider.createToken(
                member.getMemberId(),
                member.getSystemRole()
        ));

        return result;
    }
}