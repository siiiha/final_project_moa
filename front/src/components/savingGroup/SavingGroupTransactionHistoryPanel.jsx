import { useEffect, useState } from 'react';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import { getSavingGroupTransactions } from '../../api/savingGroups';

const Panel = styled.section`
  margin-top: 22px;
`;

const Header = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
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

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const TypeBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $type, theme }) => ($type === '출금' ? theme.colors.danger : theme.colors.primary)};
  background: ${({ $type, theme }) => ($type === '출금' ? theme.colors.dangerSoft : theme.colors.primarySoft)};
  font-size: 10px;
`;

const Amount = styled.strong`
  color: ${({ $type, theme }) => ($type === '출금' ? theme.colors.danger : theme.colors.primary)};
  font-size: 12px;
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
  margin-top: 10px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
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

const formatWon = (amount) => `${amount.toLocaleString('ko-KR')}원`;

// AI작성: 장부 목적과 카드 분류 코드에 표시용 이름을 적용한다.
const purposeLabels = { DUES: '회비', PAYOUT: '계금 지급', EXPENSE: '일반 지출' };
const categoryLabels = {
  FOOD: '식비', TRANSPORTATION: '교통비', RENTAL: '대관료', SUPPLIES: '물품비',
  ACTIVITY: '활동비', ACCOMMODATION: '숙박비', FEE: '수수료', OTHER: '기타',
  NEEDS_REVIEW: '확인 필요',
};

function getCategoryLabel({ purposeType, category, classificationStatus, transactionType }) {
  if (purposeType === 'DUES' || purposeType === 'PAYOUT') return purposeLabels[purposeType];
  if (classificationStatus === 'REVIEW_REQUIRED') return '확인 필요';
  return categoryLabels[category] ?? (transactionType === 'WITHDRAWAL' ? '미분류' : '—');
}

// AI작성: 서버 페이지를 표시하고 이전 모임·페이지의 응답은 취소한다.
function SavingGroupTransactionHistoryPanel({ savingGroupId }) {
  const [page, setPage] = useState(1);
  const [result, setResult] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getSavingGroupTransactions(savingGroupId, { page, signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setResult(data);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      });
    return () => controller.abort();
  }, [savingGroupId, page]);

  const loadPage = (nextPage) => {
    setResult(null);
    setFailed(false);
    setPage(nextPage);
  };
  return <SavingGroupTransactionHistoryView result={result} failed={failed} page={page} loadPage={loadPage} />;
}

// AI작성: 조회 결과에 따라 PC·모바일 목록과 안내 상태를 렌더링한다.
export function SavingGroupTransactionHistoryView({ result, failed, page, loadPage }) {
  const transactions = (result?.items ?? []).map((row) => ({
    date: row.transactedAt.replace('T', ' '),
    type: row.transactionType === 'WITHDRAWAL' ? '출금' : '입금',
    amount: formatWon(row.transactionType === 'WITHDRAWAL' ? -Math.abs(row.amount) : row.amount),
    balance: formatWon(row.balanceAfter),
    purpose: purposeLabels[row.purposeType] ?? '목적 미확인',
    category: getCategoryLabel(row),
  }));
  const totalPages = result?.meta.totalPages ?? 0;

  return (
    <Panel>
      <Header>
        <div>
          <Title>거래 내역</Title>
          <Description>공용 통장의 입출금과 거래 후 잔액을 확인할 수 있어요.</Description>
        </div>
      </Header>

      <ListCard>
        <ListTitle>입출금 목록</ListTitle>
        {failed && <Empty role="alert">거래내역을 불러오지 못했어요.</Empty>}
        {!failed && !result && <Empty role="status">거래내역을 불러오는 중이에요.</Empty>}
        {result && !transactions.length && <Empty>아직 거래내역이 없어요.</Empty>}
        {transactions.length > 0 && (
          <>
            <TableWrap>
              <Table>
                <thead>
                  <tr>
                    <th scope="col">일시</th>
                    <th scope="col">구분</th>
                    <th scope="col">거래 목적</th>
                    <th scope="col">분류 항목</th>
                    <th scope="col">금액</th>
                    <th scope="col">거래 후 잔액</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map((transaction, index) => (
                    <tr key={index}>
                      <td>{transaction.date}</td>
                      <td><TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge></td>
                      <td>{transaction.purpose}</td>
                      <td>{transaction.category}</td>
                      <td><Amount $type={transaction.type}>{transaction.amount}</Amount></td>
                      <td>{transaction.balance}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {transactions.map((transaction, index) => (
                <MobileItem key={index}>
                  <MobileTop>
                    <TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge>
                    <Amount $type={transaction.type}>{transaction.amount}</Amount>
                  </MobileTop>
                  <MobileInfo>
                    <dt>일시</dt><dd>{transaction.date}</dd>
                    <dt>거래 목적</dt><dd>{transaction.purpose}</dd>
                    <dt>분류 항목</dt><dd>{transaction.category}</dd>
                    <dt>거래 후 잔액</dt><dd>{transaction.balance}</dd>
                  </MobileInfo>
                </MobileItem>
              ))}
            </MobileList>
          </>
        )}

        {totalPages > 1 && (
          <Pagination aria-label="거래 내역 페이지 이동">
            <PageButton type="button" $variant="secondary" $size="small" disabled={page === 1} onClick={() => loadPage(page - 1)}>
              이전
            </PageButton>
            <span aria-current="page">{page} / {totalPages}</span>
            <PageButton type="button" $variant="secondary" $size="small" disabled={page >= totalPages} onClick={() => loadPage(page + 1)}>
              다음
            </PageButton>
          </Pagination>
        )}
      </ListCard>
    </Panel>
  );
}

export default SavingGroupTransactionHistoryPanel;
