package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// 카카오 토큰 응답 중 사용자 정보 조회에 필요한 값만 받음
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoTokenResponse {

    // A카카오 API 호출용 토큰이며 모아 로그인 JWT와는 별개
    @JsonProperty("access_token")
    private String accessToken;
}