package com.nightstudy.moa.member.dto;

import lombok.Getter;

@Getter
public class IdentityVerificationStartResponse {

    private final String identityVerificationId;
    private final String storeId;
    private final String channelKey;

    public IdentityVerificationStartResponse(
            String identityVerificationId,
            String storeId,
            String channelKey
    ) {
        this.identityVerificationId = identityVerificationId;
        this.storeId = storeId;
        this.channelKey = channelKey;
    }
}
