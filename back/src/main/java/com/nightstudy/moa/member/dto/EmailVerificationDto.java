package com.nightstudy.moa.member.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EmailVerificationDto {

    private Long verificationId;
    private String email;
    private String codeHash;
    private int failedAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;

    // 인증 완료 토큰과 사용 상태
    private String verificationTokenHash;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime consumedAt;
}
