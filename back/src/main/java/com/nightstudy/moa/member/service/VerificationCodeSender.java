package com.nightstudy.moa.member.service;

public interface VerificationCodeSender {

    void send(String email, String code);
}