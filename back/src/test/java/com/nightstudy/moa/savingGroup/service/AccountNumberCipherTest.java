package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.common.config.AccountEncryptionProperties;
import com.nightstudy.moa.common.exception.ApiException;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

// AI작성: 매 테스트에서 생성한 임시 키와 가상 원문으로 변조·다른 모임 복호화를 차단한다.
class AccountNumberCipherTest {
    static AccountNumberCipher cipher() {
        var props = new AccountEncryptionProperties();
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        props.setKeyId("test"); props.setKey(Base64.getEncoder().encodeToString(key));
        return new AccountNumberCipher(props);
    }
    @Test void randomizesAndAuthenticatesGroup() {
        var cipher = cipher();
        String encrypted = cipher.encrypt(1L, "X000000000000");
        assertThat(encrypted).isNotEqualTo(cipher.encrypt(1L, "X000000000000"));
        assertThat(cipher.decrypt(1L, encrypted)).isEqualTo("X000000000000");
        assertThat(cipher.masked(1L, encrypted)).isEqualTo("*********0000");
        assertThatThrownBy(() -> cipher.decrypt(2L, encrypted)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> cipher.decrypt(1L, encrypted.substring(0, encrypted.length() - 5) + "AAAAA"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> cipher().decrypt(1L, encrypted)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsMissingKey() {
        assertThatThrownBy(() -> new AccountNumberCipher(new AccountEncryptionProperties()).requireConfigured())
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("ACCOUNT_ENCRYPTION_NOT_CONFIGURED"));
    }
}
