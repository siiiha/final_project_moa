import { useEffect, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import Badge from '../common/Badge';
import { getSavingGroupPages, getSavingGroupPayoutRequests } from '../../api/savingGroups';

// AI작성: 기존 계금 요청 탭을 조회 전용으로 연결하고 큰 글씨·모바일 한 열 배치를 적용한다.
const Panel = styled.section`
  margin-top: 20px; padding: 20px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  h2 { margin: 0 0 12px; font-size: 22px; }
  p { line-height: 1.6; }
`;
const Toolbar = styled.div`
  display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 20px 0;
  select { min-height: 44px; padding: 8px 12px; font-size: 16px; }
`;
const List = styled.ul`
  list-style: none; margin: 0; padding: 0; display: grid; gap: 16px;
`;
const Item = styled.li`
  padding: 20px; border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  h3 { margin: 0 0 12px; font-size: 18px; }
  dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; margin: 16px 0 0; }
  dt { color: ${({ theme }) => theme.colors.secondaryText}; font-size: 16px; }
  dd { margin: 6px 0 0; overflow-wrap: anywhere; font-size: 18px; }
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 16px;
    dl { grid-template-columns: 1fr; }
  }
`;
const Pagination = styled.nav`
  margin-top: 20px; display: flex; flex-wrap: wrap; gap: 8px;
`;
const labels = { PENDING: '대기', APPROVED: '승인', REJECTED: '반려', COMPLETED: '완료' };
const dateLabel = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '미검토';

// AI작성: 화면 상태와 조회 동작을 분리해 빈 결과·실패·유형별 공개 필드를 렌더링 검증할 수 있게 한다.
export function SavingGroupPayoutRequestContent({ groupType, items = [], meta, page = 1, status = '',
  isLoading, errorMessage, onStatusChange, onPageChange, onRetry }) {
  const states = groupType === '순번계' ? ['PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'] : ['PENDING', 'APPROVED', 'REJECTED'];
  const pages = getSavingGroupPages(page, meta?.totalPages || 0);
  return (
    <Panel aria-busy={isLoading}>
      <h2>{groupType === '친목계' ? '지출 요청 목록' : '계금 요청 목록'}</h2>
      <Badge>계주 전용</Badge>
      <Toolbar>
        <label htmlFor="payout-status">요청 상태</label>
        <select id="payout-status" value={status} onChange={(event) => onStatusChange(event.target.value)}>
          <option value="">전체</option>
          {states.map((value) => <option key={value} value={value}>{labels[value]}</option>)}
        </select>
        {!isLoading && !errorMessage && <strong>요청 {meta?.total || 0}건</strong>}
      </Toolbar>
      {isLoading ? <p role="status">요청 목록을 불러오는 중입니다.</p>
        : errorMessage ? <div role="alert"><p>{errorMessage}</p><Button type="button" onClick={onRetry}>다시 시도</Button></div>
          : !items.length ? <div><p>해당 조건의 요청이 없습니다.</p>
            {page > 1 && <Button type="button" onClick={() => onPageChange(1)}>첫 페이지로</Button>}</div>
            : <List>{items.map((request) => (
              <Item key={request.id}>
                <h3>{request.applicant}님의 {request.type} 요청</h3>
                <Badge>{request.status}</Badge>
                <dl>
                  <div><dt>요청 금액</dt><dd>{request.amount}</dd></div>
                  <div><dt>요청일</dt><dd>{dateLabel(request.requestedAt)}</dd></div>
                  {request.roundNumber != null && <div><dt>회차</dt><dd>{request.roundNumber}회차</dd></div>}
                  {request.title != null && <div><dt>지출 제목</dt><dd>{request.title}</dd></div>}
                  {request.hasEvidence != null && <div><dt>증빙</dt><dd>{request.hasEvidence ? '첨부 있음' : '첨부 없음'}</dd></div>}
                  {request.roundNumber != null && <>
                    <div><dt>검토일</dt><dd>{dateLabel(request.reviewedAt)}</dd></div>
                    <div><dt>검토자</dt><dd>{request.reviewerName || (request.reviewedAt ? '이름 비공개' : '미검토')}</dd></div>
                  </>}
                </dl>
              </Item>
            ))}</List>}
      {!isLoading && !errorMessage && meta?.totalPages > 1 && (
        <Pagination aria-label="계금 요청 목록 페이지 이동">
          <Button type="button" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>이전</Button>
          {pages.map((value) => <Button key={value} type="button" aria-current={page === value ? 'page' : undefined}
            $variant={page === value ? 'primary' : 'secondary'} onClick={() => onPageChange(value)}>{value}</Button>)}
          <Button type="button" disabled={page >= meta.totalPages} onClick={() => onPageChange(page + 1)}>다음</Button>
        </Pagination>
      )}
    </Panel>
  );
}

function SavingGroupPayoutApprovalList({ savingGroupId, groupType }) {
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [result, setResult] = useState(null);
  const key = `${savingGroupId}:${groupType}:${page}:${status}:${refresh}`;
  const current = result?.key === key ? result : null;
  useEffect(() => {
    const controller = new AbortController();
    getSavingGroupPayoutRequests(savingGroupId, { page, status, signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) setResult({ key, ...data }); })
      .catch((error) => { if (!controller.signal.aborted) setResult({ key, error: error.message }); });
    return () => controller.abort();
  }, [savingGroupId, groupType, page, status, key]);
  return <SavingGroupPayoutRequestContent groupType={groupType} page={page} status={status}
    items={current?.items} meta={current?.meta} isLoading={!current} errorMessage={current?.error}
    onStatusChange={(value) => { setStatus(value); setPage(1); }}
    onPageChange={setPage} onRetry={() => setRefresh((value) => value + 1)} />;
}

export default SavingGroupPayoutApprovalList;
