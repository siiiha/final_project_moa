package com.nightstudy.moa.member.service;

public interface EmailVerificationService {

    Long sendVerificationCode(String email);
    Long sendVerificationCode(String email, String purpose);

    // 인증 성공 시 회원가입용 토큰 반환
    String verifyCode(Long verificationRequestId, String code);

    // 회원가입 시 인증 완료 토큰을 확인하고 사용 처리
    void consumeVerificationToken(String email, String token);
    void consumeVerificationToken(String email, String token, String purpose);

    // 이메일 인증 후 가입 계정의 존재 여부 확인
    boolean findAccount(String email, String token);
}