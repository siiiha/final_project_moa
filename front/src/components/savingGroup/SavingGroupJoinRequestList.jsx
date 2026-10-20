import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import SavingGroupPayoutApprovalList from './SavingGroupPayoutApprovalList';

const Filters = styled.nav`
  display: flex;
  gap: 5px;
  overflow-x: auto;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
`;

const FilterButton = styled.button`
  min-width: max-content;
  min-height: 34px;
  padding: 7px 13px;
  position: relative;
  color: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primarySoft : 'transparent')};
  border: 0;
  border-radius: ${({ theme }) => `${theme.radius.sm} ${theme.radius.sm} 0 0`};
  font-size: 12px;
  font-weight: ${({ $active }) => ($active ? 800 : 600)};
  white-space: nowrap;

  &::after {
    width: 100%;
    height: 2px;
    position: absolute;
    right: 0;
    bottom: -1px;
    left: 0;
    content: '';
    background: ${({ $active, theme }) => ($active ? theme.colors.primary : 'transparent')};
  }
`;

const Card = styled.section`
  margin-top: 12px;
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

const Count = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const BulkApproveButton = styled(Button)`
  min-height: 34px;
  padding: 7px 12px;
  font-size: 12px;
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
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(2),
  td:nth-child(4),
  td:nth-child(5) {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Applicant = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const VerificationBadge = styled(Badge)`
  margin-top: 5px;
  min-height: 20px;
  padding: 3px 6px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 9px;
`;

const StatusBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $status, theme }) => ($status === '거절' ? theme.colors.danger : $status === '대기' ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $status, theme }) => ($status === '거절' ? theme.colors.dangerSoft : $status === '대기' ? theme.colors.warningSoft : theme.colors.primarySoft)};
  font-size: 10px;
`;

const ActionGroup = styled.div`
  display: flex;
  justify-content: center;
  gap: 5px;
`;

const ActionButton = styled(Button)`
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
`;

const MobileApplicant = styled.div`
  display: flex;
  align-items: center;
  gap: 7px;
`;

const MobileInfo = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  line-height: 1.6;
`;

const MobileActions = styled(ActionGroup)`
  margin-top: 12px;
  justify-content: flex-end;
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

const filters = [
  { id: '전체', label: '전체' },
  { id: '대기', label: '대기' },
  { id: '승인', label: '승인' },
  { id: '거절', label: '거절' },
  { id: '계금 요청', label: '계금 요청' },
];

function RequestActions({ request, onApprove, onReject }) {
  if (request.status !== '대기') {
    return <ActionButton type="button" $variant="secondary">상세 보기</ActionButton>;
  }

  return (
    <ActionGroup>
      <ActionButton type="button" onClick={() => onApprove(request.id)}>승인</ActionButton>
      <ActionButton type="button" $variant="secondary" onClick={() => onReject(request.id)}>거절</ActionButton>
    </ActionGroup>
  );
}

function SavingGroupJoinRequestList({
  requests,
  activeFilter,
  onFilterChange,
  selectedIds,
  onToggleAll,
  onToggleRequest,
  onApprove,
  onReject,
  onBulkApprove,
  currentPage,
  totalPages,
  onPageChange,
  totalCount,
  payoutRequests,
  payoutCurrentPage,
  payoutTotalPages,
  onPayoutPageChange,
  payoutTotalCount,
  onPayoutReview,
  reviewApplicant,
}) {
  const selectableRequests = requests.filter((request) => request.status === '대기');
  const isAllSelected = selectableRequests.length > 0 && selectableRequests.every((request) => selectedIds.includes(request.id));

  return (
    <>
      <Filters aria-label="가입 신청 상태 필터">
        {filters.map((filter) => (
          <FilterButton
            key={filter.id}
            type="button"
            $active={activeFilter === filter.id}
            aria-current={activeFilter === filter.id ? 'page' : undefined}
            onClick={() => onFilterChange(filter.id)}
          >
            {filter.label}
          </FilterButton>
        ))}
      </Filters>

      {activeFilter === '계금 요청' ? (
        <SavingGroupPayoutApprovalList
          requests={payoutRequests}
          currentPage={payoutCurrentPage}
          totalPages={payoutTotalPages}
          onPageChange={onPayoutPageChange}
          totalCount={payoutTotalCount}
          onReview={onPayoutReview}
          reviewApplicant={reviewApplicant}
        />
      ) : (
        <>
          <Card>
        <CardHeader>
          <Count>신청자 {totalCount}명</Count>
          <BulkApproveButton type="button" disabled={!selectedIds.length} onClick={onBulkApprove}>
            선택 승인
          </BulkApproveButton>
        </CardHeader>

        {requests.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="7%" />
                  <col width="17%" />
                  <col width="20%" />
                  <col width="25%" />
                  <col width="10%" />
                  <col width="21%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">
                      <Checkbox id="join-request-all" aria-label="대기 신청 전체 선택" checked={isAllSelected} onChange={onToggleAll} />
                    </th>
                    <th scope="col">신청자</th>
                    <th scope="col">지역·관심사</th>
                    <th scope="col">신청 메시지</th>
                    <th scope="col">상태</th>
                    <th scope="col">처리</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td>
                        <Checkbox
                          id={`join-request-${request.id}`}
                          aria-label={`${request.applicant} 선택`}
                          checked={selectedIds.includes(request.id)}
                          disabled={request.status !== '대기'}
                          onChange={() => onToggleRequest(request.id)}
                        />
                      </td>
                      <td>
                        <Applicant>{request.applicant}</Applicant>
                        <VerificationBadge>{request.verification}</VerificationBadge>
                      </td>
                      <td>{request.regionInterest}</td>
                      <td>{request.message}</td>
                      <td><StatusBadge $status={request.status}>{request.status}</StatusBadge></td>
                      <td><RequestActions request={request} onApprove={onApprove} onReject={onReject} /></td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {requests.map((request) => (
                <MobileItem key={request.id}>
                  <MobileTop>
                    <MobileApplicant>
                      <Checkbox
                        id={`mobile-join-request-${request.id}`}
                        aria-label={`${request.applicant} 선택`}
                        checked={selectedIds.includes(request.id)}
                        disabled={request.status !== '대기'}
                        onChange={() => onToggleRequest(request.id)}
                      />
                      <div>
                        <Applicant>{request.applicant}</Applicant>
                        <VerificationBadge>{request.verification}</VerificationBadge>
                      </div>
                    </MobileApplicant>
                    <StatusBadge $status={request.status}>{request.status}</StatusBadge>
                  </MobileTop>
                  <MobileInfo>{request.regionInterest}<br />{request.message}</MobileInfo>
                  <MobileActions><RequestActions request={request} onApprove={onApprove} onReject={onReject} /></MobileActions>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>해당 상태의 가입 신청이 없습니다.</Empty>
        )}
          </Card>

          {totalPages > 1 && (
            <Pagination aria-label="가입 신청 목록 페이지 이동">
              <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>
                이전
              </PageButton>
              {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
                <PageButton
                  key={page}
                  type="button"
                  $variant={page === currentPage ? 'primary' : 'secondary'}
                  $size="small"
                  aria-current={page === currentPage ? 'page' : undefined}
                  onClick={() => onPageChange(page)}
                >
                  {page}
                </PageButton>
              ))}
              <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => onPageChange(currentPage + 1)}>
                다음
              </PageButton>
            </Pagination>
          )}
        </>
      )}
    </>
  );
}

export default SavingGroupJoinRequestList;
