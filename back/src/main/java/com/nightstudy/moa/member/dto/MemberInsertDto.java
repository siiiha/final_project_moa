package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemberInsertDto {

    private Long memberId;
    private Long regionId;
    private String email;
    private String passwordHash;
    private String name;
    private String nickname;
    private String phone;
    private LocalDate birthDate;
    private String gender;
}