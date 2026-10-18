package com.nightstudy.moa.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 로그아웃한 토큰의 저장 및 조회를 담당
@Mapper
public interface RevokedTokenMapper {

    // 해당 토큰이 로그아웃 기록에 있는지 조회
    int countByTokenId(
            @Param("tokenId") String tokenId
    );

    // 로그아웃한 토큰의 고유 번호와 만료 시각 저장
    int insertRevokedToken(
            @Param("tokenId") String tokenId,
            @Param("expiresAtMs") Long expiresAtMs
    );
}