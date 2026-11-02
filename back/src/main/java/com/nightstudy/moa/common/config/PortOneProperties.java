package com.nightstudy.moa.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone.identity")
public class PortOneProperties {

    private boolean enabled = false;

    private String storeId;

    private String channelKey;

    private String apiSecret;
}
