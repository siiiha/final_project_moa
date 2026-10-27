package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

// 발급한 토큰 서버 내부에서 전달
@Getter
public class MemberAuthTokenDto {
    private final String accessToken;

    // refresh token JSON 응답에 노출하지 않고 쿠키만 전달
    @JsonIgnore
    private final String refreshToken;

    private final long expiresAtMs;

    public MemberAuthTokenDto(
            String accessToken,
            String refreshToken,
            long expiresAtMs
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAtMs = expiresAtMs;
    }
}
