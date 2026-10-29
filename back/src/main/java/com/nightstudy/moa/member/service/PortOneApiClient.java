package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.config.PortOneProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.PortOneIdentityVerificationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

// 서버 전용 비밀키로 포트원 본인인증 결과를 조회
@Component
public class PortOneApiClient {

    private final PortOneProperties properties;
    private final RestClient restClient;

    public PortOneApiClient(PortOneProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.portone.io")
                .requestFactory(requestFactory)
                .build();
    }

    public PortOneIdentityVerificationResponse getIdentityVerification(
            String identityVerificationId
    ) {
        // 비활성 상태이거나 설정이 없으면 외부 호출을 막음
        if (!properties.isEnabled()
                || !StringUtils.hasText(properties.getStoreId())
                || !StringUtils.hasText(properties.getChannelKey())
                || !StringUtils.hasText(properties.getApiSecret())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "IDENTITY_VERIFICATION_NOT_CONFIGURED",
                    "본인인증 서비스를 준비 중입니다."
            );
        }

        if (identityVerificationId == null
                || !identityVerificationId.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDENTITY_VERIFICATION_ID",
                    "본인인증 요청 정보가 올바르지 않습니다."
            );
        }

        PortOneIdentityVerificationResponse response;

        try {
            response = restClient.get()
                    .uri(builder -> builder
                            .path("/identity-verifications/{id}")
                            .queryParam("storeId", properties.getStoreId())
                            .build(identityVerificationId))
                    .header(
                            "Authorization",
                            "PortOne " + properties.getApiSecret()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PortOneIdentityVerificationResponse.class);
        } catch (RestClientException exception) {
            // 외부 응답의 개인정보와 비밀키는 노출하지 않음
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PORTONE_VERIFICATION_LOOKUP_FAILED",
                    "인증 결과를 확인하지 못했습니다. 잠시 후 다시 시도해주세요."
            );
        }

        if (response == null
                || !identityVerificationId.equals(response.getId())
                || !StringUtils.hasText(response.getStatus())) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "PORTONE_VERIFICATION_RESPONSE_INVALID",
                    "본인인증 응답을 확인할 수 없습니다."
            );
        }

        return response;
    }
}