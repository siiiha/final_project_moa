package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그아웃한 토큰을 DB에 기록하는 서비스 구현체
@Service
public class LogoutServiceImpl implements LogoutService {

    private final RevokedTokenMapper revokedTokenMapper;

    public LogoutServiceImpl(RevokedTokenMapper revokedTokenMapper) {
        this.revokedTokenMapper = revokedTokenMapper;
    }

    // 검증된 토큰의 고유 번호와 만료 시각 저장
    @Override
    @Transactional
    public void logout(String tokenId, Long expiresAtMs) {
        revokedTokenMapper.insertRevokedToken(tokenId, expiresAtMs);
    }
}