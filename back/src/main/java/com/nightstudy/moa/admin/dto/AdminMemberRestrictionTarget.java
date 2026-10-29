package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 직접 제한 처리 중 잠근 회원의 현재 상태와 종료 시각만 전달한다.
public class AdminMemberRestrictionTarget {
    private Long memberId;
    private String accountStatus;
    private LocalDateTime suspendedUntil;
}
