package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.MemberRestrictionType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
// AI작성: 신고 처리와 관리자 직접 처리에서 동일한 회원 제한 기간 계산 규칙을 제공한다.
public class MemberRestrictionPolicy {

    // AI작성: 기존 제한보다 짧아지지 않고 영구 제한이 기간제로 바뀌지 않도록 실제 종료 시각을 계산한다.
    public LocalDateTime calculateSuspendedUntil(
            String accountStatus,
            LocalDateTime currentSuspendedUntil,
            String restrictionTypeValue,
            LocalDateTime requestedAt
    ) {
        MemberRestrictionType restrictionType = MemberRestrictionType.from(restrictionTypeValue);
        if (restrictionType.isPermanent()) {
            return null;
        }

        // AI작성: 종료 시각이 없는 SUSPENDED 회원은 기간 제한을 다시 적용해도 영구 제한을 유지한다.
        if ("SUSPENDED".equals(accountStatus) && currentSuspendedUntil == null) {
            return null;
        }

        LocalDateTime requestedUntil = requestedAt.plusDays(restrictionType.getDays());

        // AI작성: 새 제한이 기존 제한보다 짧으면 현재 종료 시각을 유지한다.
        if (currentSuspendedUntil != null && currentSuspendedUntil.isAfter(requestedUntil)) {
            return currentSuspendedUntil;
        }
        return requestedUntil;
    }
}
