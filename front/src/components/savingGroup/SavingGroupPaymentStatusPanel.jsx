import { useMemo, useState } from 'react';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import { savingGroupPayments } from '../../mocks/savingGroupPayments';

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

const HistoryButton = styled(Button)`
  min-height: 29px;
  padding: 5px 8px;
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

const MobileAction = styled.div`
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const HistoryNotice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
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

const PAGE_SIZE = 10;

function SavingGroupPaymentStatusPanel() {
  const [selectedMonth, setSelectedMonth] = useState('2026-10');
  const [onlyOverdue, setOnlyOverdue] = useState(false);
  const [selectedMember, setSelectedMember] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredPayments = useMemo(
    () => (onlyOverdue ? savingGroupPayments.filter((payment) => payment.status === '미납') : savingGroupPayments),
    [onlyOverdue],
  );
  const totalPages = Math.max(1, Math.ceil(filteredPayments.length / PAGE_SIZE));
  const visiblePayments = filteredPayments.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const paidCount = savingGroupPayments.filter((payment) => payment.status === '납부').length;
  const overdueCount = savingGroupPayments.filter((payment) => payment.status === '미납').length;
  const totalAmount = paidCount * 30000;

  const handleHistoryClick = (memberName) => {
    // TODO: Spring Boot 서버에서 선택한 계원의 회비 납부 이력을 조회합니다.
    setSelectedMember(memberName);
  };

  const handleOnlyOverdueChange = (event) => {
    setOnlyOverdue(event.target.checked);
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
  };

  return (
    <Panel>
      <Title>회비 납부 현황</Title>
      <FilterBar>
        <MonthField>
          조회 월
          <MonthInput
            type="month"
            value={selectedMonth}
            onChange={(event) => {
              setSelectedMonth(event.target.value);
              setCurrentPage(1);
            }}
          />
        </MonthField>
        <Checkbox
          id="only-overdue-payments"
          label="미납만 보기"
          checked={onlyOverdue}
          onChange={handleOnlyOverdueChange}
        />
      </FilterBar>

      <Summary>
        <SummaryCard>
          <SummaryLabel>납부</SummaryLabel>
          <SummaryValue>{paidCount}명</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>미납</SummaryLabel>
          <SummaryValue $warning>{overdueCount}명</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>이번 달 입금액</SummaryLabel>
          <SummaryValue>{totalAmount.toLocaleString()}원</SummaryValue>
        </SummaryCard>
      </Summary>

      <ListCard>
        <ListTitle>계원별 납부 현황</ListTitle>
        {visiblePayments.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="22%" />
                  <col width="18%" />
                  <col width="18%" />
                  <col width="20%" />
                  <col width="22%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">이름</th>
                    <th scope="col">월 회비</th>
                    <th scope="col">납부 여부</th>
                    <th scope="col">납부일</th>
                    <th scope="col">납부내역</th>
                  </tr>
                </thead>
                <tbody>
                  {visiblePayments.map((payment) => (
                    <tr key={payment.id}>
                      <td><Name>{payment.memberName}</Name></td>
                      <td>{payment.monthlyDues}</td>
                      <td><StatusBadge $status={payment.status}>{payment.status}</StatusBadge></td>
                      <td>{payment.paidAt}</td>
                      <td>
                        <HistoryButton type="button" $variant="secondary" onClick={() => handleHistoryClick(payment.memberName)}>
                          이력 보기
                        </HistoryButton>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {visiblePayments.map((payment) => (
                <MobileItem key={payment.id}>
                  <MobileTop>
                    <Name>{payment.memberName}</Name>
                    <StatusBadge $status={payment.status}>{payment.status}</StatusBadge>
                  </MobileTop>
                  <MobileInfo>
                    <dt>월 회비</dt>
                    <dd>{payment.monthlyDues}</dd>
                    <dt>납부일</dt>
                    <dd>{payment.paidAt}</dd>
                  </MobileInfo>
                  <MobileAction>
                    <HistoryButton type="button" $variant="secondary" onClick={() => handleHistoryClick(payment.memberName)}>
                      이력 보기
                    </HistoryButton>
                  </MobileAction>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>해당 월에 미납한 계원이 없습니다.</Empty>
        )}
        {selectedMember && <HistoryNotice role="status">{selectedMember}님의 납부 이력을 조회할 예정입니다.</HistoryNotice>}
      </ListCard>
      {totalPages > 1 && (
        <Pagination aria-label="납부 현황 페이지 이동">
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
    </Panel>
  );
}

export default SavingGroupPaymentStatusPanel;
