package com.nightstudy.moa.member.mapper;

import com.nightstudy.moa.member.dto.EmailVerificationDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

@Mapper
public interface EmailVerificationMapper {

    int insertVerification(EmailVerificationDto verification);

    EmailVerificationDto findLatestByEmail(
            @Param("email") String email,
            @Param("purpose") String purpose
    );

    EmailVerificationDto findById(
            @Param("verificationId") Long verificationId
    );

    int markVerified(
            @Param("verificationId") Long verificationId
    );

    int incrementFailedAttempts(
            @Param("verificationId") Long verificationId
    );

    // 인증 완료 시간과 회원가입용 토큰을 함께 저장
    int markVerifiedWithToken(
            @Param("verificationId") Long verificationId,
            @Param("tokenHash") String tokenHash,
            @Param("tokenExpiresAt") LocalDateTime tokenExpiresAt
    );

    // 이메일에 맞는 유효한 토큰을 한 번만 사용 처리
    int consumeVerificationToken(
            @Param("email") String email,
            @Param("tokenHash") String tokenHash,
            @Param("purpose") String purpose
    );
}