package com.nightstudy.moa.admin.service;

import com.nightstudy.moa.admin.dto.MemberRestrictionType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
// AI작성: 신고 처리와 관리자 직접 처리에서 동일한 회원 제한 기간 계산 규칙을 제공한다.
public class MemberRestrictionPolicy {

    // AI작성: 기존 기간 제한에는 새 제한 기간을 누적하고, 영구 제한은 기간제로 바뀌지 않도록 종료 시각을 계산한다.
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

        // AI작성: 아직 끝나지 않은 제한은 기존 종료 시각부터 새 기간을 더하고, 만료된 제한은 요청 시각부터 계산한다.
        LocalDateTime restrictionBaseTime = currentSuspendedUntil != null
                && currentSuspendedUntil.isAfter(requestedAt)
                ? currentSuspendedUntil
                : requestedAt;
        return restrictionBaseTime.plusDays(restrictionType.getDays());
    }
}
