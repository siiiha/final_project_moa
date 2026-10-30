package com.nightstudy.moa.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

// AI작성: 직접 제한과 신고 제한이 공유하는 기간 누적 규칙을 검증한다.
class MemberRestrictionPolicyTest {
    private final MemberRestrictionPolicy policy = new MemberRestrictionPolicy();
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 29, 10, 0);

    @Test
    void calculatesSevenAndThirtyDayRestrictions() {
        assertThat(policy.calculateRestrictedUntil(false, null, "7_DAYS", now))
                .isEqualTo(now.plusDays(7));
        assertThat(policy.calculateRestrictedUntil(false, null, "30_DAYS", now))
                .isEqualTo(now.plusDays(30));
    }

    @Test
    void accumulatesNewPeriodAfterExistingRestrictionAndKeepsPermanentRestriction() {
        LocalDateTime existing = now.plusDays(30);
        assertThat(policy.calculateRestrictedUntil(true, existing, "7_DAYS", now))
                .isEqualTo(existing.plusDays(7));
        assertThat(policy.calculateRestrictedUntil(true, null, "7_DAYS", now)).isNull();
        assertThat(policy.calculateRestrictedUntil(false, null, "PERMANENT", now)).isNull();
    }

    @Test
    void calculatesNewRestrictionFromRequestTimeWhenExistingRestrictionExpired() {
        LocalDateTime expired = now.minusDays(1);

        assertThat(policy.calculateRestrictedUntil(true, expired, "7_DAYS", now))
                .isEqualTo(now.plusDays(7));
    }
}
