package com.nightstudy.moa.admin.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// AI작성: 직접 제한 처리 중 잠근 회원 상태와 선택한 기능의 현재 제한 정보를 전달한다.
public class AdminMemberRestrictionTarget {
    private Long memberId;
    private String accountStatus;
    private MemberRestrictionScope restrictionScope;
    private LocalDateTime restrictedUntil;

    // AI작성: LEFT JOIN 결과의 범위 값으로 현재 기능 제한 행의 존재 여부를 판단한다.
    public boolean isCurrentlyRestricted() {
        return restrictionScope != null;
    }
}
