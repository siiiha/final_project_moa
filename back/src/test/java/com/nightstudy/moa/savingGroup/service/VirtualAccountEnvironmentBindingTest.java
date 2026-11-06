package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import com.nightstudy.moa.common.config.AccountEncryptionProperties;
import com.nightstudy.moa.common.config.TossVirtualAccountProperties;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;

// AI작성: 안내할 환경 변수 이름이 실제 Spring 속성에 바인딩되는지 임시 값으로 검증한다.
class VirtualAccountEnvironmentBindingTest {
    @Test void bindsDocumentedEnvironmentVariableNames() {
        byte[] key = new byte[32]; new SecureRandom().nextBytes(key);
        String temporarySecret = "test_sk_" + UUID.randomUUID().toString().replace("-", "");
        // AI작성: Boot가 실제 운영체제 환경 변수로 인식하는 표준 소스 이름을 사용한다.
        var source = new SystemEnvironmentPropertySource("systemEnvironment", Map.of(
                "MOA_TOSS_VIRTUALACCOUNT_ENABLED", "true",
                "MOA_TOSS_VIRTUALACCOUNT_SECRETKEY", temporarySecret,
                "MOA_TOSS_VIRTUALACCOUNT_BANKCODE", "20",
                "MOA_TOSS_VIRTUALACCOUNT_ACCOUNTHOLDERNAME", "모아테스트",
                "MOA_ACCOUNT_ENCRYPTION_KEYID", "test",
                "MOA_ACCOUNT_ENCRYPTION_KEY", Base64.getEncoder().encodeToString(key)));
        var binder = new Binder(ConfigurationPropertySources.from(source));
        var toss = binder.bind("moa.toss.virtual-account", Bindable.of(TossVirtualAccountProperties.class)).get();
        assertThat(toss.isEnabled()).isTrue();
        assertThat(toss.getSecretKey()).isEqualTo(temporarySecret);
        assertThat(toss.getBankCode()).isEqualTo("20");
        assertThat(toss.getAccountHolderName()).isEqualTo("모아테스트");
        var encryption = binder.bind("moa.account.encryption", Bindable.of(AccountEncryptionProperties.class)).get();
        assertThat(encryption.getKeyId()).isEqualTo("test");
        assertThat(Base64.getDecoder().decode(encryption.getKey())).hasSize(32);
    }
}
