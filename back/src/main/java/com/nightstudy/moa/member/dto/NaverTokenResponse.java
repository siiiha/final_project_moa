package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// 네이버에서 발급한 API 호출용 토큰을 받음
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
}