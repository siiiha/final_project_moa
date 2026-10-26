package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.SocialLoginMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberSocialAccountMapper {

    // 제공사와 제공사 사용자 ID로 연결된 회원 찾음

    SocialLoginMemberDto findByProviderAndProviderUserId(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );

    // 모아 회원과 인증된 소셜 계정의 연결 정보를 저장
    int insertSocialAccount(
            @Param("memberId") Long memberId,
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );
}
