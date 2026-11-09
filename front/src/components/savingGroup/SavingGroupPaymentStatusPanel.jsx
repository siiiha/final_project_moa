import { useEffect, useState } from 'react';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import { formatSavingGroupAmount, getSavingGroupPaymentStatus } from '../../api/savingGroups';

// AI작성: 기존 납부 현황의 카드, 배지, 반응형 표 스타일을 복원한다.
const Panel = styled.section`
  margin-top: 22px;
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.9px;
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.section`
  min-height: 76px;
  padding: 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const SummaryValue = styled.strong`
  margin-top: 7px;
  display: block;
  color: ${({ $warning, theme }) => ($warning ? theme.colors.warning : theme.colors.primary)};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const ListCard = styled.section`
  margin-top: 14px;
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 14px;
  }
`;

const ListTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const TableWrap = styled.div`
  margin-top: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: none;
  }
`;

const Table = styled.table`
  width: 100%;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-collapse: separate;
  border-spacing: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  table-layout: fixed;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    text-align: center;
    vertical-align: middle;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 11px;
    font-weight: 700;
  }

  td:first-child {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Name = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const StatusBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $status, theme }) => ($status === '미납' ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $status, theme }) => ($status === '미납' ? theme.colors.warningSoft : theme.colors.primarySoft)};
  font-size: 10px;
`;

const MobileList = styled.div`
  margin-top: 14px;
  display: none;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: grid;
  }
`;

const MobileItem = styled.article`
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const MobileTop = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
`;

const MobileInfo = styled.dl`
  margin-top: 11px;
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 7px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-weight: 700;
  }
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Pagination = styled.nav`
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 4px;
`;

const PageButton = styled(Button)`
  min-width: 30px;
`;

// AI작성: 한국 시간의 연·월을 월 선택 입력의 초기값으로 사용한다.
function currentMonth() {
  const parts = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit',
  }).formatToParts(new Date());
  return `${parts.find((part) => part.type === 'year').value}-${parts.find((part) => part.type === 'month').value}`;
}

export default function SavingGroupPaymentStatusPanel({ groupId }) {
  const [month, setMonth] = useState(currentMonth);
  const [page, setPage] = useState(1);
  const [request, setRequest] = useState(null);
  const [retry, setRetry] = useState(0);
  const key = `${groupId}/${month}/${page}/${retry}`;

  // AI작성: 월이나 페이지가 바뀌면 이전 요청을 취소하고 이전 결과를 숨긴다.
  useEffect(() => {
    if (!month) return undefined;
    const controller = new AbortController();
    getSavingGroupPaymentStatus(groupId, { month, page, signal: controller.signal })
      .then((result) => {
        if (!controller.signal.aborted) setRequest({ key, result });
      })
      .catch(() => {
        if (!controller.signal.aborted) setRequest({ key, error: true });
      });
    return () => controller.abort();
  }, [groupId, key, month, page]);

  const current = request?.key === key ? request : null;
  const items = current?.result?.items ?? [];
  const totalPages = current?.result?.meta?.totalPages ?? 0;


  // AI작성: 현재 페이지의 입금 합계와 상태를 표시한다.
  const paidItems = items.filter((item) => item.paid === true);
  const unpaidCount = items.filter((item) => item.paid === false).length;
  const paidAmount = items.reduce((sum, item) => sum + Number(item.depositedAmount), 0);
  const payments = items.map((item) => ({
    ...item,
    status: item.paid == null ? '회비 기준 확인 필요' : item.paid ? '납부' : '미납',
    monthlyDues: item.duesAmount == null ? '—' : formatSavingGroupAmount(item.duesAmount),
    deposited: formatSavingGroupAmount(item.depositedAmount),
    remaining: item.remainingAmount == null ? '—' : formatSavingGroupAmount(item.remainingAmount),
  }));

  return (
    <Panel aria-label="회비 납부 현황">
      <Title>회비 납부 현황</Title>
      <FilterBar>
        <MonthField>
          조회 월
          <MonthInput type="month" min="0001-01" max="9999-12" value={month} onChange={(event) => {
            setMonth(event.target.value);
            setPage(1);
          }} />
        </MonthField>
      </FilterBar>
      {current?.result && (
        <Summary aria-label="현재 페이지 회비 요약">
          <SummaryCard>
            <SummaryLabel>이 페이지 납부</SummaryLabel>
            <SummaryValue>{paidItems.length}건</SummaryValue>
          </SummaryCard>
          <SummaryCard>
            <SummaryLabel>이 페이지 미납</SummaryLabel>
            <SummaryValue $warning>{unpaidCount}건</SummaryValue>
          </SummaryCard>
          <SummaryCard>
            <SummaryLabel>이 페이지 입금 합계</SummaryLabel>
            <SummaryValue>{formatSavingGroupAmount(paidAmount)}</SummaryValue>
          </SummaryCard>
        </Summary>
      )}
      <ListCard>
        <ListTitle>계원별 납부 현황</ListTitle>
        {!month && <Empty>조회할 월을 선택해 주세요.</Empty>}
        {month && !current && <Empty role="status">납부 현황을 불러오는 중입니다.</Empty>}
        {current?.error && (
          <div role="alert">
            <Empty>납부 현황을 불러오지 못했습니다.</Empty>
            <Button type="button" onClick={() => setRetry(retry + 1)}>다시 시도</Button>
          </div>
        )}
        {current?.result && payments.length === 0 && <Empty role="status">조회 결과가 없습니다.</Empty>}
        {payments.length > 0 && (
          <>
            <TableWrap>
              <Table aria-label="계원별 월간 납부 현황">
                <thead><tr>
                  <th scope="col">이름</th><th scope="col">월 회비</th><th scope="col">입금 합계</th>
                  <th scope="col">부족액</th><th scope="col">납부 여부</th>
                </tr></thead>
                <tbody>
                  {payments.map((payment, index) => (
                    <tr key={index}>
                      <td><Name>{payment.memberName}</Name></td>
                      <td>{payment.monthlyDues}</td>
                      <td>{payment.deposited}</td>
                      <td>{payment.remaining}</td>
                      <td><StatusBadge $status={payment.status}>{payment.status}</StatusBadge></td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>
            <MobileList>
              {payments.map((payment, index) => (
                <MobileItem key={index}>
                  <MobileTop>
                    <Name>{payment.memberName}</Name>
                    <StatusBadge $status={payment.status}>{payment.status}</StatusBadge>
                  </MobileTop>
                  <MobileInfo>
                    <dt>월 회비</dt><dd>{payment.monthlyDues}</dd>
                    <dt>입금 합계</dt><dd>{payment.deposited}</dd>
                    <dt>부족액</dt><dd>{payment.remaining}</dd>
                  </MobileInfo>
                </MobileItem>
              ))}
            </MobileList>
          </>
        )}
      </ListCard>
      {current?.result && (totalPages > 1 || page > 1) && (
        <Pagination aria-label="납부 현황 페이지 이동">
          <PageButton type="button" $variant="secondary" $size="small" disabled={page <= 1} onClick={() => setPage(page - 1)}>이전</PageButton>
          <span>{page} / {Math.max(totalPages, 1)} 페이지</span>
          <PageButton type="button" $variant="secondary" $size="small" disabled={page >= totalPages} onClick={() => setPage(page + 1)}>다음</PageButton>
        </Pagination>
      )}
    </Panel>
  );
}
