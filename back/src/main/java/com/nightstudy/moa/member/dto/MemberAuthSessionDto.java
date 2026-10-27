package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MemberAuthSessionDto {

    private String sessionId;

    private Long memberId;

    private Long expiresAtMs;

    private LocalDateTime revokedAt;
}
