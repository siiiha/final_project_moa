package com.nightstudy.moa.member.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SocialLoginMemberDto {

    // 모아 회원 번호
    private Long memberId;

    private String accountStatus;

    private String systemRole;

    // 탈퇴한 회원의 로그인과 신규 가입 오인 처리 방지
    private LocalDateTime deletedAt;
}
