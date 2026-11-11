import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import SavingGroupPayoutApprovalList from './SavingGroupPayoutApprovalList';
import { getSavingGroupPages } from '../../api/savingGroups';

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

  /* AI작성: 신청자·이메일·지역의 제목과 내용을 같은 시작점에 맞추고 나머지 열은 가운데 정렬한다. */
  th:nth-child(2),
  td:nth-child(2),
  th:nth-child(3),
  td:nth-child(3),
  th:nth-child(4),
  td:nth-child(4) {
    text-align: left;
  }

  /* AI작성: 긴 이메일 주소도 열 너비 안에서 줄바꿈해 전체를 표시한다. */
  td:nth-child(3) {
    overflow-wrap: anywhere;
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
  /* AI작성: 모바일에서도 긴 이메일 원문을 생략하지 않고 줄바꿈한다. */
  overflow-wrap: anywhere;
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

// AI작성: 대기 신청만 승인·거절할 수 있고 처리 중에는 같은 화면의 추가 처리를 차단한다.
function RequestActions({ request, onApprove, onReject, pendingId, pendingAction, blocked }) {
  if (request.status !== '대기') {
    return <ActionButton type="button" $variant="secondary" disabled title="상세 조회 준비 중">상세 보기</ActionButton>;
  }

  const isPending = pendingId === request.id;
  return (
    <ActionGroup>
      <ActionButton type="button" disabled={blocked} aria-busy={isPending && pendingAction === 'approve'} onClick={() => onApprove(request.id)}>
        {isPending && pendingAction === 'approve' ? '승인 중…' : '승인'}
      </ActionButton>
      <ActionButton type="button" $variant="secondary" disabled={blocked} aria-busy={isPending && pendingAction === 'reject'} onClick={() => onReject(request.id)}>
        {isPending && pendingAction === 'reject' ? '거절 중…' : '거절'}
      </ActionButton>
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
  isLoading,
  errorMessage,
  pendingId,
  pendingAction,
  onRetry,
  onFirstPage,
  savingGroupId,
  groupType,
}) {
  const selectableRequests = requests.filter((request) => request.status === '대기');
  const isAllSelected = selectableRequests.length > 0 && selectableRequests.every((request) => selectedIds.includes(request.id));
  // AI작성: 실패·로딩 중의 이전 목록으로 승인할 수 없으며 페이지 번호는 기존 5개 묶음 규칙을 재사용한다.
  const blocked = Boolean(pendingId) || isLoading || Boolean(errorMessage);
  const pages = getSavingGroupPages(currentPage, totalPages);

  return (
    <>
      <Filters aria-label="가입 신청 상태 필터">
        {filters.map((filter) => (
          <FilterButton
            key={filter.id}
            type="button"
            disabled={Boolean(pendingId)}
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
          key={`${savingGroupId}:${groupType}`}
          savingGroupId={savingGroupId}
          groupType={groupType}
        />
      ) : (
        <>
          <Card>
        <CardHeader>
          <Count>{isLoading ? '신청 목록을 불러오는 중입니다.' : errorMessage ? '가입 신청 목록' : `신청자 ${totalCount}명`}</Count>
          {/* AI작성: TODO: 선택 승인 정책·API 확정 후 연결한다. */}
          <BulkApproveButton type="button" disabled title="선택 승인 준비 중" onClick={onBulkApprove}>
            선택 승인
          </BulkApproveButton>
        </CardHeader>

        {isLoading ? <Empty role="status">가입 신청 목록을 불러오고 있습니다.</Empty> : errorMessage ? (
          <Empty role="alert">{errorMessage}<br />
            <Button type="button" $variant="secondary" disabled={Boolean(pendingId)} onClick={onRetry}>다시 시도</Button>
          </Empty>
        ) : requests.length ? (
          <>
            <TableWrap>
              <Table>
                <colgroup>
                  <col width="7%" />
                  <col width="25%" />
                  <col width="25%" />
                  {/* AI작성: 처리 열의 여유 폭을 지역 열에 배분해 긴 지역명의 줄바꿈을 줄인다. */}
                  <col width="20%" />
                  <col width="10%" />
                  <col width="13%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">
                      <Checkbox id="join-request-all" aria-label="대기 신청 전체 선택" checked={isAllSelected} disabled={blocked} onChange={onToggleAll} />
                    </th>
                    <th scope="col">신청자</th>
                    {/* AI작성: 신청 메시지 대신 이메일을 지역 앞에 표시한다. */}
                    <th scope="col">이메일</th>
                    <th scope="col">지역</th>
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
                          disabled={blocked || request.status !== '대기'}
                          onChange={() => onToggleRequest(request.id)}
                        />
                      </td>
                      <td>
                        <Applicant>이름: {request.applicant}</Applicant>
                        {/* AI작성: 이름 원문과 닉네임을 구분해 표시한다. */}
                        <div>닉네임: {request.nickname}</div>
                        <VerificationBadge>{request.verification}</VerificationBadge>
                      </td>
                      <td>{request.email}</td>
                      <td>{request.regionInterest}</td>
                      <td><StatusBadge $status={request.status}>{request.status}</StatusBadge></td>
                      <td><RequestActions request={request} onApprove={onApprove} onReject={onReject} pendingId={pendingId} pendingAction={pendingAction} blocked={blocked} /></td>
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
                        disabled={blocked || request.status !== '대기'}
                        onChange={() => onToggleRequest(request.id)}
                      />
                      <div>
                        <Applicant>이름: {request.applicant}</Applicant>
                        {/* AI작성: 모바일에서도 PC와 같은 신청자 정보를 제공한다. */}
                        <div>닉네임: {request.nickname}</div>
                        <VerificationBadge>{request.verification}</VerificationBadge>
                      </div>
                    </MobileApplicant>
                    <StatusBadge $status={request.status}>{request.status}</StatusBadge>
                  </MobileTop>
                  {/* AI작성: 모바일도 이메일·지역 순서로 표시하며 신청 메시지는 제외한다. */}
                  <MobileInfo>이메일: {request.email}<br />지역: {request.regionInterest}</MobileInfo>
                  <MobileActions><RequestActions request={request} onApprove={onApprove} onReject={onReject} pendingId={pendingId} pendingAction={pendingAction} blocked={blocked} /></MobileActions>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>{currentPage > 1 ? '현재 페이지에 가입 신청이 없습니다.' : '해당 상태의 가입 신청이 없습니다.'}
            {currentPage > 1 && <><br /><Button type="button" $variant="secondary" onClick={onFirstPage}>첫 페이지로</Button></>}
          </Empty>
        )}
          </Card>

          {!isLoading && !errorMessage && totalPages > 1 && (
            <Pagination aria-label="가입 신청 목록 페이지 이동">
              <PageButton type="button" $variant="secondary" $size="small" disabled={blocked || currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>
                이전
              </PageButton>
              {pages.map((page) => (
                <PageButton
                  key={page}
                  type="button"
                  disabled={blocked}
                  $variant={page === currentPage ? 'primary' : 'secondary'}
                  $size="small"
                  aria-current={page === currentPage ? 'page' : undefined}
                  onClick={() => onPageChange(page)}
                >
                  {page}
                </PageButton>
              ))}
              <PageButton type="button" $variant="secondary" $size="small" disabled={blocked || currentPage >= totalPages} onClick={() => onPageChange(currentPage + 1)}>
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
