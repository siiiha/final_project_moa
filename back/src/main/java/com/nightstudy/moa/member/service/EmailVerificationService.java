package com.nightstudy.moa.member.service;

public interface EmailVerificationService {

    Long sendVerificationCode(String email);

    // AI작성: 인증 성공 시 회원가입용 토큰 반환
    String verifyCode(Long verificationRequestId, String code);

    // AI작성: 회원가입 시 인증 완료 토큰을 확인하고 사용 처리
    void consumeVerificationToken(String email, String token);
}