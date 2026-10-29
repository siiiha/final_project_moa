import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const Filters = styled.nav`
  margin-top: 16px;
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
    color: ${({ theme }) => theme.colors.secondaryText};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 12px;
    text-align: center;
    vertical-align: middle;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(2),
  td:nth-child(3),
  td:nth-child(4) {
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
  font-size: 13px;
  text-align: center;
`;

const ApprovalError = styled.p`
  margin-top: 12px;
  padding: 10px 12px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.5;
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

const filters = ['전체', '대기', '승인', '거절'];

function RequestActions({ request, onApprove, isApproving }) {
  if (request.status !== '대기') {
    return <ActionButton type="button" $variant="secondary" disabled>처리 완료</ActionButton>;
  }

  return (
    <ActionGroup>
      <ActionButton
        type="button"
        disabled={isApproving}
        aria-busy={isApproving}
        onClick={() => onApprove(request.id)}
      >
        {isApproving ? '승인 중...' : '승인'}
      </ActionButton>
      {/* AI작성: 거절 API 정책은 이번 범위에 없으므로 오작동하는 로컬 상태 변경 대신 비활성화한다. */}
      <ActionButton type="button" $variant="secondary" disabled title="가입 신청 거절 기능 준비 중">거절</ActionButton>
    </ActionGroup>
  );
}

function SmallGroupJoinRequestList({
  requests,
  activeFilter,
  onFilterChange,
  currentPage,
  totalPages,
  onPageChange,
  totalCount,
  isLoading,
  errorMessage,
  onRetry,
  onApprove,
  approvingRequestId,
  approvalError,
}) {
  return (
    <>
      <Filters aria-label="소모임 가입 신청 상태 필터">
        {filters.map((filter) => (
          <FilterButton
            key={filter}
            type="button"
            $active={activeFilter === filter}
            aria-current={activeFilter === filter ? 'page' : undefined}
            disabled={isLoading}
            onClick={() => onFilterChange(filter)}
          >
            {filter}
          </FilterButton>
        ))}
      </Filters>

      <Card>
        <CardHeader>
          <Count>{isLoading ? '신청 목록을 불러오는 중입니다.' : errorMessage ? '가입 신청 목록' : `신청자 ${totalCount}명`}</Count>
          <BulkApproveButton type="button" disabled title="선택 승인 준비 중">
            선택 승인
          </BulkApproveButton>
        </CardHeader>

        {approvalError && <ApprovalError role="alert">{approvalError}</ApprovalError>}

        {isLoading ? <Empty role="status">가입 신청 목록을 불러오고 있습니다.</Empty> : errorMessage ? (
          <Empty role="alert">
            {errorMessage}<br />
            <Button type="button" $variant="secondary" onClick={onRetry}>다시 시도</Button>
          </Empty>
        ) : requests.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="20%" />
                  <col width="20%" />
                  <col width="25%" />
                  <col width="15%" />
                  <col width="20%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">신청자</th>
                    <th scope="col">지역</th>
                    <th scope="col">신청 메시지</th>
                    <th scope="col">상태</th>
                    <th scope="col">처리</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id}>
                      <td><Applicant>{request.applicant}</Applicant></td>
                      <td>{request.regionInterest}</td>
                      <td>{request.message}</td>
                      <td><StatusBadge $status={request.status}>{request.status}</StatusBadge></td>
                      <td>
                        <RequestActions
                          request={request}
                          onApprove={onApprove}
                          isApproving={approvingRequestId === request.id}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </TableWrap>

            <MobileList>
              {requests.map((request) => (
                <MobileItem key={request.id}>
                  <MobileTop>
                    <MobileApplicant><Applicant>{request.applicant}</Applicant></MobileApplicant>
                    <StatusBadge $status={request.status}>{request.status}</StatusBadge>
                  </MobileTop>
                  <MobileInfo>{request.regionInterest}<br />{request.message}</MobileInfo>
                  <MobileActions>
                    <RequestActions
                      request={request}
                      onApprove={onApprove}
                      isApproving={approvingRequestId === request.id}
                    />
                  </MobileActions>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : <Empty>해당 상태의 가입 신청이 없습니다.</Empty>}
      </Card>

      {!isLoading && !errorMessage && totalPages > 1 && (
        <Pagination aria-label="소모임 가입 신청 목록 페이지 이동">
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>이전</PageButton>
          {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
            <PageButton key={page} type="button" $variant={page === currentPage ? 'primary' : 'secondary'} $size="small" aria-current={page === currentPage ? 'page' : undefined} onClick={() => onPageChange(page)}>{page}</PageButton>
          ))}
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => onPageChange(currentPage + 1)}>다음</PageButton>
        </Pagination>
      )}
    </>
  );
}

export default SmallGroupJoinRequestList;
