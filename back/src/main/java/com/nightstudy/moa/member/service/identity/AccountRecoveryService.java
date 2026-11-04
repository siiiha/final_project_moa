package com.nightstudy.moa.member.service.identity;

public interface AccountRecoveryService {

    StartResponse start();

    java.util.List<FoundAccount> complete(String verificationId, String requestToken);

    record FoundAccount(String email, String provider) {}

    record StartResponse(
            String identityVerificationId,
            String requestToken,
            String storeId,
            String channelKey
    ) {}
}