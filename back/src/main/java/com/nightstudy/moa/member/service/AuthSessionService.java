package com.nightstudy.moa.member.service;

import com.nightstudy.moa.member.dto.MemberAuthTokenDto;

public interface AuthSessionService {

    // 기존 로그인 검증결과로 새 로그인 세션 만듦
    MemberAuthTokenDto startSession(String accessToken);

    // refresh token 검증 후 새 토큰으로 교체
    MemberAuthTokenDto refresh(String refreshToken);

    // 토큰에 연결된 로그인 세션 폐기
    void logout(String refreshToken, String accessToken);

    //  회원의 모든 로그인 세션을 폐기
    void revokeAllSessions(Long memberId);

    // 현재 회원 상태 로그인 세션 유효성 확인
    boolean isAccessAllowed(
            Long memberId,
            String systemRole,
            String sessionId
    );
}
