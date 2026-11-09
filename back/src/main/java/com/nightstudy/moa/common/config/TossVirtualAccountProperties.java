package com.nightstudy.moa.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AI작성: 테스트 전용 설정이며 은행과 예금주는 상점 확인 후 환경 변수로 주입한다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "moa.toss.virtual-account")
public class TossVirtualAccountProperties {
    private boolean enabled;
    private String secretKey;
    private String bankCode;
    private String accountHolderName;
}
