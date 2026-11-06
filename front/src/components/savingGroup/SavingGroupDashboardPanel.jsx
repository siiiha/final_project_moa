import { FiArrowRight } from 'react-icons/fi';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import styled from 'styled-components';
import Button from '../common/Button';
import { savingGroupTransactions } from '../../mocks/savingGroupTransactions';
import { savingGroupDashboardSummary } from '../../mocks/savingGroupDashboard';

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

const ViewAllButton = styled(Button)`
  min-height: 28px;
  padding: 4px 0;
  color: ${({ theme }) => theme.colors.primary};
  border-color: transparent;
  font-size: 11px;

  &:hover {
    color: ${({ theme }) => theme.colors.primaryDark};
    background: transparent;
  }
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

const RecentTable = styled.table`
  width: 100%;
  margin-top: 14px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-collapse: separate;
  border-spacing: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  table-layout: fixed;

  th,
  td {
    padding: 9px 7px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 11px;
    text-align: left;
    vertical-align: middle;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 10px;
    font-weight: 700;
  }

  td:nth-child(3),
  td:nth-child(4) {
    text-align: right;
    white-space: nowrap;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    th,
    td {
      padding: 9px 5px;
      font-size: 10px;
    }

    th {
      font-size: 9px;
    }
  }
`;

const Amount = styled.strong`
  color: ${({ $type, theme }) => ($type === '출금' ? theme.colors.danger : theme.colors.primary)};
  font-size: inherit;
`;

const Note = styled.p`
  margin-top: 14px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  line-height: 1.6;
`;

const formatAmount = (amount) => `${amount > 0 ? '+' : '-'}${Math.abs(amount).toLocaleString()}원`;
const formatBalance = (balance) => `${balance.toLocaleString()}원`;
const formatCurrency = (amount) => `${amount.toLocaleString()}원`;

function SavingGroupDashboardPanel({ onViewTransactions }) {
  // AI작성: 최근 거래는 거래 내역 화면과 같은 공용 통장 거래 Mock을 기준으로 표시한다.
  const recentTransactions = savingGroupTransactions.slice(0, 3);
  // AI작성: 목록 정렬에 의존하지 않고 가장 최근 거래의 잔액만 대시보드 현재 잔액으로 사용한다.
  const latestTransaction = savingGroupTransactions.reduce((latest, transaction) => (
    !latest || `${transaction.date} ${transaction.time}` > `${latest.date} ${latest.time}` ? transaction : latest
  ), null);
  // AI작성: Mock은 과거 기간 데이터이므로 가장 최근 거래가 속한 월을 현재 조회 기준으로 사용한다.
  const referenceMonth = latestTransaction?.date.slice(0, 7);
  const monthlyDues = savingGroupTransactions
    .filter((transaction) => (
      referenceMonth != null
      && transaction.date.startsWith(referenceMonth)
      && transaction.type === '입금'
      && transaction.purposeType === '회비'
    ))
    .reduce((total, transaction) => total + transaction.amount, 0);
  // AI작성: 계금 지급과 아직 확인하지 않은 출금은 모임 운영 지출 통계에서 제외한다.
  const monthlyExpense = savingGroupTransactions
    .filter((transaction) => (
      referenceMonth != null
      && transaction.date.startsWith(referenceMonth)
      && transaction.purposeType === '일반 지출'
      && transaction.category !== '확인 필요'
    ))
    .reduce((total, transaction) => total + Math.abs(transaction.amount), 0);
  const reviewRequiredCount = savingGroupTransactions.filter((transaction) => (
    transaction.purposeType === '일반 지출' && transaction.category === '확인 필요'
  )).length;
  // AI작성: 차트 단위가 만 원이므로 회비 입금과 확정 일반 지출을 월별로 합산해 변환한다.
  const monthlyCashFlow = [...new Set(savingGroupTransactions.map((transaction) => transaction.date.slice(0, 7)))]
    .sort()
    .map((month) => {
      const monthlyTransactions = savingGroupTransactions.filter((transaction) => transaction.date.startsWith(month));
      const income = monthlyTransactions
        .filter((transaction) => transaction.type === '입금' && transaction.purposeType === '회비')
        .reduce((total, transaction) => total + transaction.amount, 0);
      const expense = monthlyTransactions
        .filter((transaction) => transaction.purposeType === '일반 지출' && transaction.category !== '확인 필요')
        .reduce((total, transaction) => total + Math.abs(transaction.amount), 0);
      return { month: `${Number(month.slice(5))}월`, income: income / 10000, expense: expense / 10000 };
    });
  const dashboardSummary = [
    ...savingGroupDashboardSummary.map((item) => (
    item.id === 'balance'
      ? { ...item, value: latestTransaction ? formatBalance(latestTransaction.balance) : '잔액 확인 필요' }
      : item.id === 'income' ? { ...item, value: formatCurrency(monthlyDues) }
        : item.id === 'expense' ? { ...item, value: formatCurrency(monthlyExpense) } : item
    )),
    { id: 'reviewRequired', label: '확인 필요 출금', value: `${reviewRequiredCount}건` },
  ];

  const handleViewTransactions = () => {
    onViewTransactions();
  };

  return (
    <Panel>
      <Heading>
        <div>
          <Title>계모임 회비 대시보드</Title>
          <Description>함께 모은 회비와 사용 내역을 투명하게 확인하세요.</Description>
        </div>
      </Heading>

      <Summary>
        {dashboardSummary.map((item) => (
          <SummaryCard key={item.id}>
            <SummaryLabel>{item.label}</SummaryLabel>
            <SummaryValue>{item.value}</SummaryValue>
          </SummaryCard>
        ))}
      </Summary>

      <DashboardGrid>
        <Card>
          <CardHeader>
            <CardTitle>수입·지출 추이</CardTitle>
          </CardHeader>
          <ChartBox aria-label="월별 수입·지출 막대 차트">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={savingGroupMonthlyCashFlow} margin={{ top: 14, right: 4, bottom: 0, left: -20 }} barGap={5}>
                <CartesianGrid vertical={false} stroke="#e3e8e6" strokeDasharray="3 3" />
                <XAxis dataKey="month" tickLine={false} axisLine={false} />
                <YAxis tickLine={false} axisLine={false} tickFormatter={(value) => `${value}만`} />
                <Tooltip formatter={(value) => `${value.toLocaleString()}만 원`} />
                <Legend iconType="circle" iconSize={7} />
                <Bar dataKey="income" name="수입" fill="#176b5b" radius={[3, 3, 0, 0]} maxBarSize={24} />
                <Bar dataKey="expense" name="지출" fill="#bdcbd0" radius={[3, 3, 0, 0]} maxBarSize={24} />
              </BarChart>
            </ResponsiveContainer>
          </ChartBox>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>최근 거래 내역</CardTitle>
            <ViewAllButton type="button" $variant="secondary" onClick={handleViewTransactions}>
              전체 거래 보기 <FiArrowRight size={13} aria-hidden="true" />
            </ViewAllButton>
          </CardHeader>
          <RecentTable>
            <colgroup>
              <col width="26%" />
              <col width="30%" />
              <col width="22%" />
              <col width="22%" />
            </colgroup>
            <thead>
              <tr>
                <th scope="col">일시</th>
                <th scope="col">거래처·가맹점명</th>
                <th scope="col">금액</th>
                <th scope="col">잔액</th>
              </tr>
            </thead>
            <tbody>
              {recentTransactions.map((transaction) => (
                <tr key={transaction.id}>
                  <td>{`${transaction.date.replaceAll('-', '.')} ${transaction.time}`}</td>
                  <td>{transaction.merchantName}</td>
                  <td><Amount $type={transaction.type}>{formatAmount(transaction.amount)}</Amount></td>
                  <td>{`${transaction.balance.toLocaleString()}원`}</td>
                </tr>
              ))}
            </tbody>
          </RecentTable>
        </Card>
      </DashboardGrid>

      <Note>수입·지출 현황은 모임 공용 통장 거래 내역을 기준으로 표시됩니다.</Note>
    </Panel>
  );
}

export default SavingGroupDashboardPanel;
