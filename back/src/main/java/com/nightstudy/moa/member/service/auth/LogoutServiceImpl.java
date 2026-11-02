package com.nightstudy.moa.member.service.auth;

import com.nightstudy.moa.common.auth.AuthStateCache;
import com.nightstudy.moa.member.mapper.RevokedTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 로그아웃한 토큰을 DB에 기록하는 서비스 구현체
@Service
public class LogoutServiceImpl implements LogoutService {

    private final RevokedTokenMapper revokedTokenMapper;
    private final AuthStateCache authStateCache;

    public LogoutServiceImpl(
            RevokedTokenMapper revokedTokenMapper,
            AuthStateCache authStateCache
    ) {
        this.revokedTokenMapper = revokedTokenMapper;
        this.authStateCache = authStateCache;
    }

    // 검증된 토큰의 고유 번호와 만료 시각 저장
    @Override
    @Transactional
    public void logout(String tokenId, Long expiresAtMs) {
        // AI작성: DB 기록 전에 캐시에 폐기 표시를 남겨 오래된 인증 캐시의 재사용을 차단한다.
        if (!authStateCache.saveRevokedToken(tokenId, expiresAtMs)) {
            throw new IllegalStateException(
                    "로그아웃 토큰 캐시 저장에 실패했습니다."
            );
        }

        revokedTokenMapper.insertRevokedToken(tokenId, expiresAtMs);
    }
}
