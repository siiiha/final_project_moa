package com.nightstudy.moa.member.service.auth;

public interface LogoutService {

    void logout(String tokenId, Long expiresAtMs);
}
