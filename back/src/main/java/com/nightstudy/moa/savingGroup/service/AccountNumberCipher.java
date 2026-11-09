package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.config.AccountEncryptionProperties;
import com.nightstudy.moa.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// AI작성: 모임 ID를 인증 데이터에 묶어 다른 모임으로 암호문이 복사되면 복호화를 거절한다.
@Component
public class AccountNumberCipher {
    private final AccountEncryptionProperties properties;
    private final SecureRandom random = new SecureRandom();
    public AccountNumberCipher(AccountEncryptionProperties properties) { this.properties = properties; }

    public void requireConfigured() { key(); }

    private byte[] key() {
        try {
            if (properties.getKeyId() == null || !properties.getKeyId().matches("[A-Za-z0-9_-]{1,24}"))
                throw new IllegalArgumentException();
            byte[] key = Base64.getDecoder().decode(properties.getKey());
            if (key.length != 32) throw new IllegalArgumentException();
            return key;
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ACCOUNT_ENCRYPTION_NOT_CONFIGURED", "계좌 암호화 설정을 확인해 주세요.");
        }
    }

    public String encrypt(Long groupId, String plain) {
        byte[] key = key();
        if (groupId == null || groupId <= 0 || plain == null || !plain.matches("(?:X)?[0-9]{4,20}")) throw invalid();
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, key, iv, groupId);
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.US_ASCII));
            return "v1:" + properties.getKeyId() + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) { throw invalid(); }
    }

    public String decrypt(Long groupId, String stored) {
        byte[] key = key();
        try {
            if (groupId == null || groupId <= 0 || stored == null) throw new IllegalArgumentException();
            String[] parts = stored.split(":", -1);
            if (parts.length != 4 || !"v1".equals(parts[0]) || !properties.getKeyId().equals(parts[1]))
                throw new IllegalArgumentException();
            byte[] iv = Base64.getDecoder().decode(parts[2]);
            if (iv.length != 12) throw new IllegalArgumentException();
            String plain = new String(cipher(Cipher.DECRYPT_MODE, key, iv, groupId)
                    .doFinal(Base64.getDecoder().decode(parts[3])), StandardCharsets.US_ASCII);
            if (!plain.matches("(?:X)?[0-9]{4,20}")) throw new IllegalArgumentException();
            return plain;
        } catch (Exception e) { throw invalid(); }
    }

    public String masked(Long groupId, String stored) {
        String plain = decrypt(groupId, stored);
        return "*".repeat(Math.max(4, plain.length() - 4)) + plain.substring(plain.length() - 4);
    }

    private Cipher cipher(int mode, byte[] key, byte[] iv, Long groupId) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(("moa-account:" + groupId).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private ApiException invalid() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT_DATA_INVALID", "계좌 정보를 처리할 수 없습니다.");
    }
}
