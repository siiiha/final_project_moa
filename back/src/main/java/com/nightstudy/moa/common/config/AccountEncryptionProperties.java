package com.nightstudy.moa.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AI작성: 비밀키는 환경 변수로만 주입하며 자동 toString을 생성하지 않는다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "moa.account.encryption")
public class AccountEncryptionProperties {
    private String keyId;
    private String key;
}
