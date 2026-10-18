package com.nightstudy.moa.member.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalVerificationCodeSender
        implements VerificationCodeSender {

    private static final Logger log =
            LoggerFactory.getLogger(LocalVerificationCodeSender.class);

    @Override
    public void send(String email, String code) {
        log.info("[로컬 개발용] 이메일 인증번호: {}", code);
    }
}