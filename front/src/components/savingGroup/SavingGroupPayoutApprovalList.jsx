import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import Badge from '../common/Badge';
import ConfirmModal from '../common/ConfirmModal';
import { approveSavingGroupPayoutRequest, rejectSavingGroupPayoutRequest, getSavingGroupPages, getSavingGroupPayoutRequests } from '../../api/savingGroups';

// AI작성: 기존 계주 전용 목록에 승인·거절 동작을 연결하고 큰 글씨·모바일 한 열 배치를 적용한다.
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
// AI작성: 거절 사유 입력과 오류 안내를 연결하고 고령 이용자가 읽기 쉬운 크기로 표시한다.
const ReasonField = styled.div`
  margin-top: 16px;
  font-size: 16px;
  label { display: block; margin-bottom: 8px; }
  textarea { width: 100%; box-sizing: border-box; min-height: 120px; padding: 12px; font: inherit; resize: vertical; }
  p { line-height: 1.6; }
`;
const labels = { PENDING: '대기', APPROVED: '승인', REJECTED: '반려', COMPLETED: '완료' };
const dateLabel = (value) => value ? String(value).replace('T', ' ').slice(0, 16) : '미검토';

// AI작성: 권한·인증·대상·요청자 상태 변경은 목록을 다시 확인한 뒤에만 심사를 재개한다.
function requiresPayoutRequestRefresh(error) {
  return [401, 403, 404].includes(error.status) || [
    'SAVING_GROUP_ACTIVITY_RESTRICTED', 'PAYOUT_GROUP_TYPE_UNSUPPORTED',
    'PAYOUT_REQUEST_ALREADY_PROCESSED', 'PAYOUT_REQUESTER_INELIGIBLE',
  ].includes(error.code);
}

// AI작성: 처리 후 전체 건수가 줄면 서버 메타를 기준으로 마지막 유효 페이지를 다시 조회한다.
function getPayoutRequestPage(page, totalPages) {
  return Math.min(page, Math.max(1, totalPages));
}

// AI작성: 화면 상태와 조회 동작을 분리해 빈 결과·실패·유형별 공개 필드를 렌더링 검증할 수 있게 한다.
export function SavingGroupPayoutRequestContent({ groupType, items = [], meta, page = 1, status = '',
  isLoading, errorMessage, onStatusChange, onPageChange, onRetry, onApprove, onReject, isApproving = false }) {
  const states = groupType === '순번계' ? ['PENDING', 'APPROVED', 'REJECTED', 'COMPLETED'] : ['PENDING', 'APPROVED', 'REJECTED'];
  const pages = getSavingGroupPages(page, meta?.totalPages || 0);
  return (
    <Panel aria-busy={isLoading}>
      <h2>{groupType === '친목계' ? '지출 요청 목록' : '계금 요청 목록'}</h2>
      <Badge>계주 전용</Badge>
      <Toolbar>
        <label htmlFor="payout-status">요청 상태</label>
        <select id="payout-status" value={status} disabled={isApproving} onChange={(event) => onStatusChange(event.target.value)}>
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
                {/* AI작성: 현재 API의 한글 상태값을 사용하고 대기 요청만 심사한다. 최종 권한·상태 검증은 서버가 수행한다. */}
                {request.status === '대기' && (onApprove || onReject) && (
                  <Toolbar>
                    {onApprove && <Button type="button" disabled={isApproving} onClick={() => onApprove(request)}>
                      승인
                    </Button>}
                    {onReject && <Button type="button" $variant="danger" disabled={isApproving} onClick={() => onReject(request)}>
                      거절
                    </Button>}
                  </Toolbar>
                )}
              </Item>
            ))}</List>}
      {!isLoading && !errorMessage && meta?.totalPages > 1 && (
        <Pagination aria-label="계금 요청 목록 페이지 이동">
          <Button type="button" disabled={isApproving || page <= 1} onClick={() => onPageChange(page - 1)}>이전</Button>
          {pages.map((value) => <Button key={value} type="button" aria-current={page === value ? 'page' : undefined}
            disabled={isApproving} $variant={page === value ? 'primary' : 'secondary'} onClick={() => onPageChange(value)}>{value}</Button>)}
          <Button type="button" disabled={isApproving || page >= meta.totalPages} onClick={() => onPageChange(page + 1)}>다음</Button>
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
  // AI작성: 렌더링 전 재클릭도 잠금으로 막고 모임 전환·언마운트 후에는 이전 요청 결과를 표시하지 않는다.
  const [approveTarget, setApproveTarget] = useState(null);
  // AI작성: 실패한 거절의 대상·입력 사유를 유지하여 같은 창에서 확인하고 재시도할 수 있게 한다.
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectError, setRejectError] = useState('');
  const [rejectUnavailable, setRejectUnavailable] = useState(false);
  const [activityError, setActivityError] = useState('');
  const [isApproving, setIsApproving] = useState(false);
  const [notice, setNotice] = useState(null);
  const approvalLock = useRef(false);
  const approvalController = useRef(null);
  useEffect(() => () => approvalController.current?.abort(), [savingGroupId, groupType]);
  const key = `${savingGroupId}:${groupType}:${page}:${status}:${refresh}`;
  const current = result?.key === key ? result : null;
  useEffect(() => {
    const controller = new AbortController();
    getSavingGroupPayoutRequests(savingGroupId, { page, status, signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted) return;
        const nextPage = getPayoutRequestPage(page, data.meta.totalPages);
        if (nextPage !== page) { setPage(nextPage); return; }
        setActivityError('');
        setResult({ key, ...data });
      })
      .catch((error) => { if (!controller.signal.aborted) setResult({ key, error: error.message }); });
    return () => controller.abort();
  }, [savingGroupId, groupType, page, status, key]);
  // AI작성: 확인 시에만 승인하고 성공 후 서버 목록을 다시 조회한다. 실제 지급 처리는 하지 않는다.
  const approveRequest = async () => {
    if (approvalLock.current || activityError || !current || current.error || !approveTarget || approveTarget.status !== '대기') return;
    approvalLock.current = true;
    setIsApproving(true);
    const controller = new AbortController();
    approvalController.current = controller;
    try {
      await approveSavingGroupPayoutRequest(savingGroupId, approveTarget.id, { signal: controller.signal });
      if (controller.signal.aborted) return;
      setApproveTarget(null);
      setNotice({ title: '요청 승인 완료', message: '요청을 승인했습니다. 실제 지급은 별도로 진행됩니다.' });
      setRefresh((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      setApproveTarget(null);
      setNotice({ title: '요청 승인 실패', message: error.message || '승인하지 못했습니다. 다시 시도해주세요.' });
      if (requiresPayoutRequestRefresh(error)) {
        setActivityError(error.message || '요청을 처리할 수 없습니다. 목록을 다시 확인해주세요.');
        setRefresh((value) => value + 1);
      }
    } finally {
      approvalLock.current = false;
      if (!controller.signal.aborted) setIsApproving(false);
    }
  };
  // AI작성: 승인과 같은 잠금을 공유해 동시 심사를 막고, 서버 성공 후에만 입력창을 닫는다.
  const rejectRequest = async () => {
    if (rejectUnavailable) { setRejectTarget(null); return; }
    if (activityError || !current || current.error) return;
    if (approvalLock.current || !rejectTarget || rejectTarget.status !== '대기') return;
    if (!rejectReason.trim() || rejectReason.length > 500) {
      setRejectError('거절 사유를 입력해주세요. 공백만 입력할 수 없으며 최대 500자입니다.');
      return;
    }
    approvalLock.current = true;
    setIsApproving(true);
    setRejectError('');
    const controller = new AbortController();
    approvalController.current = controller;
    try {
      await rejectSavingGroupPayoutRequest(savingGroupId, rejectTarget.id, rejectReason, { signal: controller.signal });
      if (controller.signal.aborted) return;
      setRejectTarget(null);
      setRejectReason('');
      setNotice({ title: '요청 거절 완료', message: '요청을 거절했습니다.' });
      setRefresh((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      setRejectError(error.message || '거절하지 못했습니다. 사유를 확인하고 다시 시도해주세요.');
      if (requiresPayoutRequestRefresh(error)) {
        setRejectUnavailable(true);
        setActivityError(error.message || '요청을 처리할 수 없습니다. 목록을 다시 확인해주세요.');
        setRefresh((value) => value + 1);
      }
    } finally {
      approvalLock.current = false;
      if (!controller.signal.aborted) setIsApproving(false);
    }
  };
  return <><SavingGroupPayoutRequestContent groupType={groupType} page={page} status={status}
    items={current?.items} meta={current?.meta} isLoading={!current} errorMessage={current?.error || activityError}
    onApprove={(request) => { if (!approvalLock.current && current && !current.error && !activityError) setApproveTarget(request); }} isApproving={isApproving}
    onReject={(request) => {
      if (approvalLock.current || !current || current.error || activityError) return;
      setRejectTarget(request); setRejectReason(''); setRejectError(''); setRejectUnavailable(false);
    }}
    onStatusChange={(value) => { if (!approvalLock.current) { setStatus(value); setPage(1); } }}
    onPageChange={(value) => { if (!approvalLock.current) setPage(value); }}
    onRetry={() => setRefresh((value) => value + 1)} />
    {approveTarget && <ConfirmModal title="요청을 승인할까요?"
      description="승인은 요청 심사 처리이며 실제 지급은 별도입니다."
      confirmLabel={isApproving ? '승인 중...' : '승인'} cancelFirst isBusy={isApproving}
      onConfirm={approveRequest} onCancel={() => { if (!approvalLock.current) setApproveTarget(null); }}>
      <p>{approveTarget.applicant}님의 {approveTarget.type} 요청</p>
      <p>요청 금액: {approveTarget.amount}</p>
    </ConfirmModal>}
    {rejectTarget && <ConfirmModal title="요청을 거절할까요?"
      description="요청을 거절하는 사유를 최대 500자로 입력해주세요."
      confirmLabel={isApproving ? '거절 중...' : rejectUnavailable ? '목록 확인' : '거절'} confirmVariant="danger" cancelFirst isBusy={isApproving}
      onConfirm={rejectRequest} onCancel={() => {
        if (approvalLock.current) return;
        setRejectTarget(null); setRejectReason(''); setRejectError('');
      }}>
      <p>{rejectTarget.applicant}님의 {rejectTarget.type} 요청</p>
      <p>요청 금액: {rejectTarget.amount}</p>
      <ReasonField>
        <label htmlFor="payout-reject-reason">거절 사유 (필수)</label>
        <textarea id="payout-reject-reason" value={rejectReason} maxLength={500} required disabled={isApproving || rejectUnavailable}
          aria-invalid={Boolean(rejectError)} aria-describedby="payout-reject-count payout-reject-error"
          onChange={(event) => { setRejectReason(event.target.value); setRejectError(''); }} />
        <p id="payout-reject-count">{rejectReason.length}/500자</p>
        <p id="payout-reject-error" role={rejectError ? 'alert' : undefined}>{rejectError}</p>
      </ReasonField>
    </ConfirmModal>}
    {notice && <ConfirmModal title={notice.title} description={notice.message} showCancel={false}
      onConfirm={() => setNotice(null)} onCancel={() => setNotice(null)} />}
  </>;
}

export default SavingGroupPayoutApprovalList;
