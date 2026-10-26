package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.config.NaverOAuthProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.NaverTokenResponse;
import com.nightstudy.moa.member.dto.NaverUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

// 네이버 인증 API와 통신하며 인증 정보는 로그에 출력하지 않음
@Component
public class NaverApiClient {

    private final NaverOAuthProperties properties;
    private final RestClient restClient;

    public NaverApiClient(NaverOAuthProperties properties) {
        this.properties = properties;

        // 연결 및 응답 대기 시간을 제한
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

    // Controller에서 state 검증을 마친 뒤 호출
    public String exchangeCodeForAccessToken(String code, String state) {
        if (code == null || code.isBlank()
                || state == null || state.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NAVER_AUTH_INFO_REQUIRED",
                    "네이버 인증 정보가 없습니다. 다시 로그인해주세요."
            );
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", code);
        form.add("state", state);

        NaverTokenResponse tokenResponse;

        try {
            tokenResponse = restClient.post()
                    .uri("https://nid.naver.com/oauth2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(NaverTokenResponse.class);
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "NAVER_TOKEN_REQUEST_FAILED",
                    "네이버 인증을 완료하지 못했습니다. 다시 로그인해주세요."
            );
        }

        if (tokenResponse == null
                || tokenResponse.getAccessToken() == null
                || tokenResponse.getAccessToken().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "NAVER_TOKEN_RESPONSE_INVALID",
                    "네이버 인증 응답을 확인할 수 없습니다."
            );
        }

        return tokenResponse.getAccessToken();
    }

    // 네이버에서 확인한 사용자 고유 ID를 반환
    public String getUserId(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NAVER_TOKEN_REQUIRED",
                    "네이버 인증 정보가 없습니다. 다시 로그인해주세요."
            );
        }

        NaverUserResponse userResponse;

        try {
            userResponse = restClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(NaverUserResponse.class);
        } catch (RestClientException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "NAVER_USER_REQUEST_FAILED",
                    "네이버 회원 정보를 확인하지 못했습니다."
            );
        }

        if (userResponse == null
                || !"00".equals(userResponse.getResultCode())
                || userResponse.getResponse() == null
                || userResponse.getResponse().getId() == null
                || userResponse.getResponse().getId().isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "NAVER_USER_RESPONSE_INVALID",
                    "네이버 회원 정보 응답이 올바르지 않습니다."
            );
        }

        return userResponse.getResponse().getId();
    }
}