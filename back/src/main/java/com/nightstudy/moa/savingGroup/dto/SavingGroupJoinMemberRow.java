package com.nightstudy.moa.savingGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 시 로그인 회원의 서버 관리 상태를 조회하는 MyBatis 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SavingGroupJoinMemberRow {
    private Long memberId;
    private String accountStatus;
    private LocalDateTime identityVerifiedAt;
}
