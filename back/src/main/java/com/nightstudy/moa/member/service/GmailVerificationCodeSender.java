package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// mail 환경에서 Gmail을 통해 인증번호 발송
@Component
@Profile("mail")
public class GmailVerificationCodeSender
        implements VerificationCodeSender {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public GmailVerificationCodeSender(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail
    ) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    @Override
    public void send(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("[모아] 이메일 인증번호");
        message.setText(
                "안녕하세요. 모아입니다.\n\n"
                        + "인증번호: " + code + "\n\n"
                        + "인증번호는 발급 후 3분 동안 유효합니다.\n"
                        + "본인이 요청하지 않았다면 이 메일을 무시해주세요."
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_SEND_FAILED",
                    "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }
}