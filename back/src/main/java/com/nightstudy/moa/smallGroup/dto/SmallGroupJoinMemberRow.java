package com.nightstudy.moa.smallGroup.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI작성: 가입 신청 중 잠금 조회한 회원의 신청 가능 상태와 시스템 권한을 Service에서 판별하는 내부 DTO다.
@Getter
@Setter
@NoArgsConstructor
public class SmallGroupJoinMemberRow {
    private Long memberId;
    private String accountStatus;
    private LocalDateTime identityVerifiedAt;
    private String systemRole;
}
