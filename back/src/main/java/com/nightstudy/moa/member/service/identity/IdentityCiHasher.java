package com.nightstudy.moa.member.service.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class IdentityCiHasher {

    private final String secret;

    public IdentityCiHasher(
            @Value("${MOA_IDENTITY_CI_HMAC_SECRET:}") String secret
    ) {
        this.secret = secret;
    }

    public boolean isConfigured() {
        return !secret.isBlank();
    }

    public String hash(String ci) {
        if (secret.isBlank()) {
            throw new IllegalStateException("CI 변환 비밀키가 설정되지 않았습니다.");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] digest = mac.doFinal(ci.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("CI 변환에 실패했습니다.", e);
        }
    }
}