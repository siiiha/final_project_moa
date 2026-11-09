package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI작성: 같은 이름도 독립 행으로 처리하고 승인 계주에게만 집계 결과를 제공한다.
class SavingGroupPaymentStatusServiceTest {
    private final SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    private final SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper,
            mock(VirtualAccountStorageMapper.class), mock(AccountNumberCipher.class), mock(NotificationService.class));
    private final YearMonth month = YearMonth.of(2026, 11);

    private void membership(String role, String status) {
        when(mapper.selectSavingGroupDetail(2L)).thenReturn(new SavingGroupRow());
        var relation = new SavingGroupMembershipRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        when(mapper.selectSavingGroupMembership(2L, 1L)).thenReturn(relation);
    }

    @ParameterizedTest
    @CsvSource({"MEMBER,APPROVED", "MANAGER,APPROVED", "OWNER,PENDING", "OWNER,LEFT"})
    void deniesOtherMemberships(String role, String status) {
        membership(role, status);
        assertThatThrownBy(() -> service.getSavingGroupPaymentStatus(2L, 1L, month, 1))
                .isInstanceOf(ApiException.class);
        verify(mapper, never()).countSavingGroupPaymentStatus(2L);
    }

    @Test
    void calculatesShortfallAndLeavesUnconfirmedHistoricalDuesUndecided() {
        membership("OWNER", "APPROVED");
        var start = month.atDay(1).atStartOfDay();
        var end = month.plusMonths(1).atDay(1).atStartOfDay();
        when(mapper.countSavingGroupPaymentStatus(2L)).thenReturn(12);
        when(mapper.selectMonthlyPaymentDues(2L, end)).thenReturn(new BigDecimal("12000")).thenReturn(null);
        when(mapper.selectSavingGroupPaymentStatus(2L, start, end, 10, 10)).thenReturn(List.of(
                new SavingGroupPaymentStatusRow("테스트", new BigDecimal("12000")),
                new SavingGroupPaymentStatusRow("테스트", new BigDecimal("2000"))));

        var result = service.getSavingGroupPaymentStatus(2L, 1L, month, 2);
        assertThat(result.pageMeta().getTotalPages()).isEqualTo(2);
        assertThat(result.items()).extracting(SavingGroupPaymentStatusResponse::paid).containsExactly(true, false);
        assertThat(result.items().get(1).remainingAmount()).isEqualByComparingTo("10000");
        assertThat(service.getSavingGroupPaymentStatus(2L, 1L, month, 2).items())
                .allSatisfy(row -> { assertThat(row.paid()).isNull(); assertThat(row.remainingAmount()).isNull(); });
        assertThat(service.getSavingGroupPaymentStatus(2L, 1L, month, 3).items()).isEmpty();
    }
}
