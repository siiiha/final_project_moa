package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.config.KakaoOAuthProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.KakaoTokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.nightstudy.moa.member.dto.KakaoUserResponse;

import java.net.http.HttpClient;
import java.time.Duration;

// 카카오 API 통신을 담당한다. 인증 정보는 로그에 출력하지 않음
@Component
public class KakaoApiClient {

    private final KakaoOAuthProperties properties;
    private final RestClient restClient;

    public KakaoApiClient(KakaoOAuthProperties properties) {
        this.properties = properties;

        // 카카오 서버 연결 지연 시 무한정 기다리지 않도록 제한
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public String exchangeCodeForAccessToken(String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_CODE_REQUIRED",
                    "카카오 인증 코드가 없습니다. 다시 로그인해주세요."
            );
        }

        // 카카오 토큰 API는 JSON이 아닌 폼 형식으로 요청
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", code);

        KakaoTokenResponse tokenResponse;

        try {
            tokenResponse = restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
        } catch (RestClientException exception) {
            // 외부 서버의 오류 본문과 인증 정보를 사용자에게 노출하지 않음
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_TOKEN_REQUEST_FAILED",
                    "카카오 인증을 완료하지 못했습니다. 다시 로그인해주세요."
            );
        }

        if (tokenResponse == null
                || tokenResponse.getAccessToken() == null
                || tokenResponse.getAccessToken().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_TOKEN_RESPONSE_INVALID",
                    "카카오 인증 응답을 확인할 수 없습니다. 다시 로그인해주세요."
            );
        }

        return tokenResponse.getAccessToken();
    }

    // 카카오 액세스 토큰으로 검증된 사용자 고유 ID를 조회
    public Long getUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "KAKAO_TOKEN_REQUIRED",
                    "카카오 인증 정보가 없습니다. 다시 로그인해주세요."
            );
        }

        KakaoUserResponse userResponse;

        try {
            userResponse = restClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);
        } catch (RestClientException exception) {
            // 외부 오류 본문이나 토큰을 응답에 포함하지 않음
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_USER_REQUEST_FAILED",
                    "카카오 회원 정보를 확인하지 못했습니다. 다시 로그인해주세요."
            );
        }

        if (userResponse == null
                || userResponse.getId() == null
                || userResponse.getId() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "KAKAO_USER_RESPONSE_INVALID",
                    "카카오 회원 정보 응답이 올바르지 않습니다."
            );
        }

        return userResponse.getId();
    }
}