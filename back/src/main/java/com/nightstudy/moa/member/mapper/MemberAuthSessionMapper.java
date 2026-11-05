package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.auth.MemberAuthSessionDto;
import com.nightstudy.moa.member.dto.auth.MemberRefreshTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nightstudy.moa.member.dto.social.SocialLoginMemberDto;

@Mapper
public interface MemberAuthSessionMapper {

    // 토큰이 속한 세션 찾아 잠금 순서 통일
    String findSessionIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    // 같은 세션의 동시 재발급과 로그아웃을 순서대로 처리
    MemberAuthSessionDto findSessionForUpdate(
            @Param("sessionId") String sessionId
    );

    // 세션 잠금을 획득한 뒤 토큰 최신 사용 상태 확인
    MemberRefreshTokenDto findTokenForUpdate(
            @Param("tokenHash") String tokenHash
    );

   SocialLoginMemberDto findMemberById(
            @Param("memberId") Long memberId
    );

    int insertSession(MemberAuthSessionDto session);

    int insertToken(MemberRefreshTokenDto token);

    // 사용하지 않은 토큰만 한 번 사용 처리
    int markTokenUsed(
            @Param("tokenHash") String tokenHash
    );

    boolean hasRecentMypageVerification(
            @Param("sessionId") String sessionId,
            @Param("memberId") Long memberId,
            @Param("nowMs") long nowMs
    );

    int markMypageVerified(
            @Param("sessionId") String sessionId,
            @Param("memberId") Long memberId,
            @Param("nowMs") long nowMs
    );

    int revokeSession(
            @Param("sessionId") String sessionId
    );

    // 비밀번호 재설정 시 해당 회원의 모든 로그인 세션을 폐기
    int revokeAllSessionsByMemberId(
            @Param("memberId") Long memberId
    );

    // AI작성: 세션·회원 상태와 로그아웃한 access token 여부를 한 번에 검사한다.

    int countActiveSession(
            @Param("sessionId") String sessionId,
            @Param("memberId") Long memberId,
            @Param("systemRole")  String systemRole,
            @Param("nowMs") long nowMs,
            @Param("tokenId") String tokenId
    );
}

