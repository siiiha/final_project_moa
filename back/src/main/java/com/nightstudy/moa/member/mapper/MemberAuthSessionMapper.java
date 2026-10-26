package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.MemberAuthSessionDto;
import com.nightstudy.moa.member.dto.MemberRefreshTokenDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.nightstudy.moa.member.dto.SocialLoginMemberDto;

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

    int revokeSession(
            @Param("sessionId") String sessionId
    );

    // 세션 유효기간과 현재 회원 상태·권한을 함께 검사

    int countActiveSession(
            @Param("sessionId") String sessionId,
            @Param("memberId") Long memberId,
            @Param("systemRole")  String systemRole,
            @Param("nowMs") long nowMs
    );
}

