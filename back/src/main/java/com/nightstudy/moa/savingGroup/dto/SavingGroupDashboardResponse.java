package com.nightstudy.moa.savingGroup.dto;

import java.math.BigDecimal;
import java.util.List;

// AI작성: 계모임 대시보드의 이번 달 요약과 최근 여섯 달 집계만 공개한다.
public record SavingGroupDashboardResponse(
        BigDecimal currentBalance,
        BigDecimal monthlyDuesTotal,
        BigDecimal monthlyExpenseTotal,
        int reviewRequiredWithdrawalCount,
        List<MonthlyTrend> monthlyTrends,
        List<ExpenseCategoryRatio> expenseCategoryRatios
) {
    public record MonthlyTrend(String month, BigDecimal income, BigDecimal expense) {}
    public record ExpenseCategoryRatio(String category, BigDecimal amount) {}
}
