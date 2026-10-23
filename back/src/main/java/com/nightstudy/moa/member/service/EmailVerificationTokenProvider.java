package com.nightstudy.moa.member.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// 회원가입용 인증 완료 토큰 생성 및 해시 변환
@Component
public class EmailVerificationTokenProvider {

    private final SecureRandom secureRandom = new SecureRandom();

    public String createToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashedBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "토큰 해시 기능을 사용할 수 없습니다.",
                    e
            );
        }
    }
}