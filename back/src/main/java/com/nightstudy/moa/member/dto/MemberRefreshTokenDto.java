package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class MemberRefreshTokenDto {

    private String tokenHash;

    private String sessionId;

    private LocalDateTime usedAt;

}
