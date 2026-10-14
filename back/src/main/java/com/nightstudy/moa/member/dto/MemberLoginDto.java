package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberLoginDto {

    private Long memberId;
    private String passwordHash;
    private String accountStatus;
    private String systemRole;
}