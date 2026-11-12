import { useEffect, useState } from 'react';
import { Bar, BarChart, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { getSavingGroupDashboard, getSavingGroupPaymentStatus, savingGroupExpenseCategoryLabels } from '../../api/savingGroups';
import styled from 'styled-components';

const Panel = styled.section`
  margin-top: 22px;
`;

const Heading = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.9px;
`;

const Description = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.6;
`;

const FilterBar = styled.div`
  margin-top: 16px;
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 10px 18px;
`;

const MonthField = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const MonthInput = styled.input`
  width: 140px;
  height: 40px;
  padding: 0 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Summary = styled.div`
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    gap: 10px;
  }
`;

const SummaryCard = styled.section`
  min-height: 82px;
  padding: 15px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-height: 74px;
    padding: 13px;
  }
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const SummaryValue = styled.strong`
  margin-top: 8px;
  display: block;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    font-size: 16px;
  }
`;

const DashboardGrid = styled.div`
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.85fr);
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Card = styled.section`
  min-width: 0;
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 14px;
  }
`;

const CardHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const CardTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.65px;
`;

const ChartBox = styled.div`
  height: 234px;
  margin-top: 14px;

  .recharts-cartesian-axis-tick-value {
    fill: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
  }

  .recharts-legend-item-text {
    color: ${({ theme }) => `${theme.colors.secondaryText} !important`};
    font-size: 11px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    height: 218px;
  }
`;

const Note = styled.p`
  margin-top: 14px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  line-height: 1.6;
`;

const EmptyChart = styled.p`
  display: grid;
  height: 100%;
  place-items: center;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  text-align: center;
`;

const koreanMonth = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit',
});

const chartColors = ['#2F80ED', '#27AE60', '#F2994A', '#9B51E0', '#EB5757', '#56CCF2', '#BB6BD9', '#6FCF97', '#828282'];

// AI작성: 소액은 원 단위로 보존하고 큰 금액만 만 원 단위로 줄여 차트 축의 반올림 오해를 막는다.
const formatChartAxisAmount = (value) => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '';
  if (Math.abs(amount) < 10000) return `${amount.toLocaleString('ko-KR')}원`;
  const tenThousand = amount / 10000;
  return `${Number.isInteger(tenThousand) ? tenThousand : tenThousand.toFixed(1)}만`;
};

// AI작성: 모든 페이지를 조회하며 회비 기준이 없는 계원이 있으면 합계 표시를 보류한다.
export async function loadOutstanding(savingGroupId, month, signal, loadPage = getSavingGroupPaymentStatus) {
  let total = 0;
  let unknown = false;
  let pages = 1;
  for (let page = 1; page <= pages; page += 1) {
    const { items, meta } = await loadPage(savingGroupId, { month, page, signal });
    signal.throwIfAborted();
    pages = meta.totalPages;
    for (const { remainingAmount } of items) {
      if (remainingAmount == null) unknown = true;
      else total += remainingAmount;
    }
  }
  return unknown ? null : total;
}

async function loadDashboard(savingGroupId, month, signal) {
  return getSavingGroupDashboard(savingGroupId, { month, signal });
}

// AI작성: 모임이나 월이 바뀌면 이전 요청을 취소하고 늦게 도착한 결과를 무시한다.
function useDashboardResult(savingGroupId, month, load) {
  const [result, setResult] = useState(null);
  useEffect(() => {
    if (savingGroupId == null) return;
    const controller = new AbortController();
    load(savingGroupId, month, controller.signal).then(
      (value) => {
        if (!controller.signal.aborted) setResult({ savingGroupId, month, value });
      },
      () => {
        if (!controller.signal.aborted) setResult({ savingGroupId, month, failed: true });
      },
    );
    return () => controller.abort();
  }, [savingGroupId, month, load]);
  return result?.savingGroupId === savingGroupId && result?.month === month ? result : null;
}

export function SavingGroupDashboardView({ dashboard, outstanding, canViewOutstanding, month, onMonthChange }) {
  const format = (value) => value == null ? '—' : value.toLocaleString('ko-KR') + '원';
  const data = dashboard?.value;
  const categoryData = (data?.expenseCategoryRatios ?? []).map((item) => ({
    ...item, label: savingGroupExpenseCategoryLabels[item.category] ?? '분류 확인 필요',
  }));
  // AI작성: 실제 0원과 미조회·오류를 구분하고 계주에게만 미납을 표시한다.
  const dashboardSummary = [
    { id: 'balance', label: '현재 잔액', value: format(data?.currentBalance) },
    { id: 'income', label: '이번 달 납부', value: format(data?.monthlyDuesTotal) },
    ...(canViewOutstanding ? [{ id: 'overdue', label: '미납', value: format(outstanding?.value) }] : []),
    { id: 'expense', label: '이번 달 지출', value: format(data?.monthlyExpenseTotal) },
    { id: 'reviewRequired', label: '확인 필요 출금', value: data ? `${data.reviewRequiredWithdrawalCount}건` : '—' },
  ];

  return (
    <Panel>
      <Heading>
        <div>
          <Title>계모임 회비 대시보드</Title>
          <Description>함께 모은 회비와 사용 내역을 투명하게 확인하세요.</Description>
        </div>
      </Heading>

      <FilterBar>
        <MonthField>
          조회 월
          <MonthInput type="month" min="0001-01" max="9999-12" value={month}
            onChange={(event) => onMonthChange(event.target.value)} />
        </MonthField>
      </FilterBar>

      <Summary>
        {dashboardSummary.map((item) => (
          <SummaryCard key={item.id}>
            <SummaryLabel>{item.label}</SummaryLabel>
            <SummaryValue>{item.value}</SummaryValue>
          </SummaryCard>
        ))}
      </Summary>

      {/* AI작성: 대시보드 집계와 계주 전용 미납 조회 상태를 독립적으로 안내한다. */}
      {!dashboard && <Note role="status">대시보드를 불러오는 중입니다.</Note>}
      {dashboard?.failed && <Note role="alert">대시보드를 불러오지 못했습니다.</Note>}
      {canViewOutstanding && !outstanding && <Note role="status">미납 합계를 불러오는 중입니다.</Note>}
      {canViewOutstanding && outstanding?.failed && <Note role="alert">미납 합계를 불러오지 못했습니다.</Note>}
      {canViewOutstanding && outstanding?.value === null && <Note>회비 기준 확인이 필요합니다.</Note>}

      <DashboardGrid>
        <Card>
          <CardHeader><CardTitle>수입·지출 추이</CardTitle></CardHeader>
          <ChartBox aria-label="월별 수입·지출 막대 차트">
            {!data ? <EmptyChart>대시보드 집계를 불러오는 중입니다.</EmptyChart> : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data.monthlyTrends} margin={{ top: 8, right: 8, left: -12, bottom: 0 }}>
                  <XAxis dataKey="month" tickFormatter={(value) => `${value.slice(5)}월`} />
                  <YAxis tickFormatter={formatChartAxisAmount} />
                  <Tooltip formatter={(value) => format(Number(value))} labelFormatter={(value) => `${value} 수입·지출`} />
                  <Legend />
                  <Bar dataKey="income" name="수입" fill="#2F80ED" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expense" name="지출" fill="#EB5757" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </ChartBox>
        </Card>
        <Card>
          <CardHeader><CardTitle>지출 항목별 비율</CardTitle></CardHeader>
          <ChartBox aria-label="지출 항목별 파이 차트">
            {!data ? <EmptyChart>대시보드 집계를 불러오는 중입니다.</EmptyChart> : !categoryData.length ? (
              <EmptyChart>이번 달 지출 내역이 없습니다.</EmptyChart>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={categoryData} dataKey="amount" nameKey="label" outerRadius={76} label={({ label }) => label}>
                    {categoryData.map((item, index) => <Cell key={item.category} fill={chartColors[index % chartColors.length]} />)}
                  </Pie>
                  <Tooltip formatter={(value) => format(Number(value))} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            )}
          </ChartBox>
        </Card>
      </DashboardGrid>

      <Note>수입·지출 현황은 모임 공용 통장 거래 내역을 기준으로 표시됩니다.</Note>
    </Panel>
  );
}

// AI작성: 한국 시간의 현재 월로 승인 계주에게만 미납을 요청한다.
export default function SavingGroupDashboardPanel({ savingGroupId, canViewOutstanding }) {
  // AI작성: 납부 현황과 동일하게 선택한 월을 대시보드 집계 기준으로 사용한다.
  const [month, setMonth] = useState(() => koreanMonth.format(new Date()));
  const dashboard = useDashboardResult(savingGroupId, month, loadDashboard);
  const outstanding = useDashboardResult(canViewOutstanding ? savingGroupId : null, month, loadOutstanding);
  return <SavingGroupDashboardView dashboard={dashboard} outstanding={outstanding}
    canViewOutstanding={canViewOutstanding} month={month} onMonthChange={setMonth} />;
}
