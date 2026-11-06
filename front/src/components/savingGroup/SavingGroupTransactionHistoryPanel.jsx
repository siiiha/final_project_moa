import { useMemo, useState } from 'react';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import { expenseCategories } from '../../constants/expenseCategories';
import { savingGroupTransactions } from '../../mocks/savingGroupTransactions';

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

const DownloadButton = styled(Button)`
  min-height: 36px;
  padding: 7px 12px;
  font-size: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const HeaderActions = styled.div`
  display: flex;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: grid;
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
  color: ${({ theme }) => theme.colors.primary};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const FilterForm = styled.form`
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(140px, 1fr) minmax(120px, 1fr) auto;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const Input = styled.input`
  width: 100%;
  height: 40px;
  padding: 0 11px;
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

const Select = styled.select`
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ $needsReview, theme }) => ($needsReview ? theme.colors.danger : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const SearchButton = styled(Button)`
  min-height: 40px;
  padding: 8px 17px;
  align-self: end;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-column: 1 / -1;
  }
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

  td:nth-child(1),
  td:nth-child(3) {
    text-align: left;
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

const MobileDescription = styled.strong`
  margin-top: 10px;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const Category = styled.span`
  color: ${({ theme }) => theme.colors.text};
  font-size: 11px;
  font-weight: 700;
`;

const PayoutCategory = styled(Category)`
  color: ${({ theme }) => theme.colors.secondaryText};
`;

const ReviewCategory = styled(Category)`
  display: inline-block;
  padding: 3px 6px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
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

const Notice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

const PAGE_SIZE = 10;
const MOCK_SAVE_DELAY = 400;

const formatAmount = (amount) => `${amount > 0 ? '+' : '-'}${Math.abs(amount).toLocaleString()}원`;

function SavingGroupTransactionHistoryPanel({ myRole }) {
  // AI작성: 실제 통장 거래 API 전까지 분류 수정 결과는 화면 상태에서만 확인한다.
  const [transactions, setTransactions] = useState(savingGroupTransactions);
  const [draftFilters, setDraftFilters] = useState({ startDate: '2026-09-01', endDate: '2026-10-03', type: '전체' });
  const [appliedFilters, setAppliedFilters] = useState(draftFilters);
  const [currentPage, setCurrentPage] = useState(1);
  const [isDownloadPreparing, setIsDownloadPreparing] = useState(false);
  const [hasCategoryChanges, setHasCategoryChanges] = useState(false);
  const [isCategorySaving, setIsCategorySaving] = useState(false);
  const [categoryMessage, setCategoryMessage] = useState('');
  const canEditCategory = myRole === 'OWNER' || myRole === 'MANAGER';

  const filteredTransactions = useMemo(() => transactions.filter((transaction) => {
    const matchesStartDate = !appliedFilters.startDate || transaction.date >= appliedFilters.startDate;
    const matchesEndDate = !appliedFilters.endDate || transaction.date <= appliedFilters.endDate;
    const matchesType = appliedFilters.type === '전체' || transaction.type === appliedFilters.type;
    return matchesStartDate && matchesEndDate && matchesType;
  }), [appliedFilters, transactions]);
  const totalPages = Math.max(1, Math.ceil(filteredTransactions.length / PAGE_SIZE));
  const visibleTransactions = filteredTransactions.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 서버에 공용 통장 거래 내역 조회 조건을 전달합니다.
    setAppliedFilters(draftFilters);
    setCurrentPage(1);
  };

  const handleDownload = () => {
    // TODO: Spring Boot 서버에서 조회 조건에 맞는 거래 내역 파일을 내려받습니다.
    setIsDownloadPreparing(true);
  };

  const handlePageChange = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
  };

  const handleCategoryChange = (transactionId, category) => {
    setTransactions((current) => current.map((transaction) => (
      transaction.id === transactionId && transaction.purposeType === '일반 지출'
        ? { ...transaction, category }
        : transaction
    )));
    setHasCategoryChanges(true);
    setCategoryMessage('');
  };

  const handleCategorySave = async () => {
    if (!hasCategoryChanges || isCategorySaving) return;
    setIsCategorySaving(true);
    // AI작성: 실제 API 연결 전에도 저장 중 상태를 보여주기 위해 Mock 응답 시간을 둔다.
    await new Promise((resolve) => window.setTimeout(resolve, MOCK_SAVE_DELAY));
    setHasCategoryChanges(false);
    setCategoryMessage('변경한 출금 분류를 저장했습니다.');
    setIsCategorySaving(false);
  };

  return (
    <Panel>
      <Header>
        <div>
          <Title>공용 통장 거래내역</Title>
          <Description>모임 자금의 입출금 내역입니다. 출금은 가맹점명과 지출 항목을 함께 확인합니다.</Description>
        </div>
        <HeaderActions>
          {canEditCategory && (
            <DownloadButton type="button" disabled={!hasCategoryChanges || isCategorySaving} onClick={handleCategorySave}>
              {isCategorySaving ? '저장 중...' : '분류 저장'}
            </DownloadButton>
          )}
          <DownloadButton type="button" $variant="secondary" onClick={handleDownload}>내역 다운로드</DownloadButton>
        </HeaderActions>
      </Header>

      <Summary>
        <SummaryCard>
          <SummaryLabel>통장 잔액</SummaryLabel>
          <SummaryValue>4,820,000원</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>조회 입금</SummaryLabel>
          <SummaryValue>30,000원</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>조회 출금</SummaryLabel>
          <SummaryValue>280,000원</SummaryValue>
        </SummaryCard>
      </Summary>

      <FilterForm onSubmit={handleSearch}>
        <Field>
          시작일
          <Input
            type="date"
            value={draftFilters.startDate}
            onChange={(event) => setDraftFilters((current) => ({ ...current, startDate: event.target.value }))}
          />
        </Field>
        <Field>
          종료일
          <Input
            type="date"
            value={draftFilters.endDate}
            onChange={(event) => setDraftFilters((current) => ({ ...current, endDate: event.target.value }))}
          />
        </Field>
        <Field>
          구분
          <Select value={draftFilters.type} onChange={(event) => setDraftFilters((current) => ({ ...current, type: event.target.value }))}>
            <option value="전체">전체</option>
            <option value="입금">입금</option>
            <option value="출금">출금</option>
          </Select>
        </Field>
        <SearchButton type="submit">조회</SearchButton>
      </FilterForm>

      <ListCard>
        <ListTitle>입출금 목록</ListTitle>
        {visibleTransactions.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="18%" />
                  <col width="12%" />
                  <col width="24%" />
                  <col width="14%" />
                  <col width="16%" />
                  <col width="16%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">일시</th>
                    <th scope="col">구분</th>
                    <th scope="col">거래처·가맹점명</th>
                    <th scope="col">분류 항목</th>
                    <th scope="col">금액</th>
                    <th scope="col">잔액</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleTransactions.map((transaction) => (
                    <tr key={transaction.id}>
                      <td>{`${transaction.date.slice(5).replace('-', '.')} ${transaction.time}`}</td>
                      <td><TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge></td>
                      {/* AI작성: 통장 적요와 별도로 가맹점명·입금자를 표시해 분류 기준을 분명히 한다. */}
                      <td>{transaction.merchantName}</td>
                      <td>
                        {transaction.purposeType === '일반 지출' && canEditCategory ? (
                          <Select
                            aria-label={`${transaction.merchantName} 지출 항목`}
                            $needsReview={transaction.category === '확인 필요'}
                            value={transaction.category}
                            onChange={(event) => handleCategoryChange(transaction.id, event.target.value)}
                          >
                            {expenseCategories.map((category) => <option key={category} value={category}>{category}</option>)}
                          </Select>
                        ) : transaction.purposeType === '계금 지급'
                          ? <PayoutCategory>계금 지급</PayoutCategory>
                          : transaction.category === '확인 필요'
                            ? <ReviewCategory>확인 필요</ReviewCategory>
                            : <Category>{transaction.category}</Category>}
                      </td>
                      <td><Amount $type={transaction.type}>{formatAmount(transaction.amount)}</Amount></td>
                      <td>{`${transaction.balance.toLocaleString()}원`}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {visibleTransactions.map((transaction) => (
                <MobileItem key={transaction.id}>
                  <MobileTop>
                    <TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge>
                    <Amount $type={transaction.type}>{formatAmount(transaction.amount)}</Amount>
                  </MobileTop>
                  <MobileDescription>{transaction.merchantName}</MobileDescription>
                  <MobileInfo>
                    <dt>일시</dt>
                    <dd>{`${transaction.date} ${transaction.time}`}</dd>
                    <dt>분류 항목</dt>
                    <dd>
                      {transaction.purposeType === '일반 지출' && canEditCategory ? (
                        <Select
                          aria-label={`${transaction.merchantName} 지출 항목`}
                          $needsReview={transaction.category === '확인 필요'}
                          value={transaction.category}
                          onChange={(event) => handleCategoryChange(transaction.id, event.target.value)}
                        >
                          {expenseCategories.map((category) => <option key={category} value={category}>{category}</option>)}
                        </Select>
                      ) : transaction.purposeType === '계금 지급'
                        ? <PayoutCategory>계금 지급</PayoutCategory>
                        : transaction.category === '확인 필요'
                          ? <ReviewCategory>확인 필요</ReviewCategory>
                          : <Category>{transaction.category}</Category>}
                    </dd>
                    <dt>잔액</dt>
                    <dd>{`${transaction.balance.toLocaleString()}원`}</dd>
                  </MobileInfo>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>조회 조건에 맞는 거래 내역이 없습니다.</Empty>
        )}

        {totalPages > 1 && (
          <Pagination aria-label="거래 내역 페이지 이동">
            <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => handlePageChange(currentPage - 1)}>
              이전
            </PageButton>
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
              <PageButton
                key={page}
                type="button"
                $variant={page === currentPage ? 'primary' : 'secondary'}
                $size="small"
                aria-current={page === currentPage ? 'page' : undefined}
                onClick={() => handlePageChange(page)}
              >
                {page}
              </PageButton>
            ))}
            <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => handlePageChange(currentPage + 1)}>
              다음
            </PageButton>
          </Pagination>
        )}
        {categoryMessage && <Notice role="status">{categoryMessage}</Notice>}
        {isDownloadPreparing && <Notice role="status">거래 내역 다운로드를 준비할 예정입니다.</Notice>}
        <Notice>계금 지급은 계원에게 돌려주는 금액으로, 일반 지출 분류·통계·학습 대상에 포함하지 않습니다.</Notice>
      </ListCard>
    </Panel>
  );
}

export default SavingGroupTransactionHistoryPanel;
