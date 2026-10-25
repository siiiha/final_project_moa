package com.nightstudy.moa.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "oauth.naver")
public class NaverOAuthProperties {

    @NotBlank(message = "네이버 Client ID를 설정해주세요.")
    private String clientId;

    @NotBlank(message = "네이버 Client Secret을 설정해주세요.")
    private String clientSecret;

    @NotBlank(message = "네이버 Callback URL을 설정해주세요.")
    private String redirectUri;
}