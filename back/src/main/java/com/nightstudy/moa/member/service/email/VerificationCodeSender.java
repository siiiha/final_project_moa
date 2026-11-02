package com.nightstudy.moa.member.service.email;

public interface VerificationCodeSender {

    void send(String email, String code);
}