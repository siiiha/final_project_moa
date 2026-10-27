package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// 네이버 사용자 조회의 결과 코드와 사용자 정보를 받음
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverUserResponse {

    @JsonProperty("resultcode")
    private String resultCode;

    private UserInfo response;

    // 네이버의 사용자 ID는 response 객체 안에 들어 있음
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserInfo {

        private String id;
    }
}