package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;

// AI작성: 순번 추천과 지정에서 같은 현재 회차와 고정 계원 기준을 사용한다.
final class SavingGroupTurnOrderPolicy {
    private SavingGroupTurnOrderPolicy() {}

    static int nextRound(String status, LocalDate startDate, LocalDate today) {
        if ("01".equals(status)) return 1;
        if (startDate == null || today.isBefore(startDate))
            throw conflict("계모임 시작일을 확인해주세요.");
        return Math.toIntExact(ChronoUnit.MONTHS.between(YearMonth.from(startDate), YearMonth.from(today)) + 2);
    }

    static <T> Set<String> fixedMembers(List<T> members, Function<T, String> memberId,
            Predicate<T> paid, Function<T, Integer> sequence, int nextRound) {
        var fixed = new HashSet<String>();
        for (var member : members) {
            if (paid.test(member)) fixed.add(memberId.apply(member));
        }
        if (nextRound > 1) {
            var recipients = members.stream()
                    .filter(member -> Objects.equals(sequence.apply(member), nextRound - 1)).toList();
            if (recipients.size() != 1) throw conflict("현재 회차의 지급 계원을 확인해주세요.");
            fixed.add(memberId.apply(recipients.get(0)));
        }
        return fixed;
    }

    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TURN_ORDER_CONFLICT", message);
    }
}
