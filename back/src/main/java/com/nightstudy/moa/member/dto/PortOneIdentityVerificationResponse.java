package com.nightstudy.moa.member.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

// 서버가 포트원에서 조회한 본인인증 결과를 받음
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOneIdentityVerificationResponse {

    private String id;
    private String status;
    private Instant verifiedAt;
    private Channel channel;
    private VerifiedCustomer verifiedCustomer;

    // 설정한 채널과 테스트 여부를 확인할 때 사용
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String key;
        private String type;
    }

    // 로그인 회원과 인증된 사람의 정보가 일치하는지 확인
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifiedCustomer {
        private String name;
        private String birthDate;
        private String phoneNumber;
    }
}