package com.nightstudy.moa.member.service;

public interface LogoutService {

    void logout(String tokenId, Long expiresAtMs);
}
