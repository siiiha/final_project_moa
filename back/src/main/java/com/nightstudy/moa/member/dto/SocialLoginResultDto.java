package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialLoginResultDto {

    // true면 추가 정보 입력, false면 기존 회원 로그인 성공
    private boolean signupRequired;

    private String token;

    private String signupToken;

    @JsonIgnore
    private String providerUserId;
}

