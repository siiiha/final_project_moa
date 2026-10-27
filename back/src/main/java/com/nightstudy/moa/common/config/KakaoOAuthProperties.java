package com.nightstudy.moa.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

// application.yaml의 oauth.kakao 설정을 읽어 주입
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoOAuthProperties {

    // REST API 키는 카카오에서 서비스를 식별하는 값
    @NotBlank(message = "카카오 REST API 키를 설정해주세요.")
    private String clientId;

    // 토큰 발급 요청에 사용하는 비밀값으로 로그에 출력하지 않음
    @NotBlank(message = "카카오 클라이언트 시크릿을 설정해주세요.")
    private String clientSecret;

    //  카카오 개발자 화면에 등록한 콜백 주소와 일치
    @NotBlank(message = "카카오 Redirect URI를 설정해주세요.")
    private String redirectUri;
}