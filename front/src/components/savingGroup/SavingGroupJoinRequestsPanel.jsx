import { useEffect, useRef, useState } from 'react';
import SavingGroupJoinRequestList from './SavingGroupJoinRequestList';
import { savingGroupPayoutRequests } from '../../mocks/savingGroupPayoutRequests';
import { approveSavingGroupJoin, getSavingGroupJoinRequests, rejectSavingGroupJoin } from '../../api/savingGroups';
import ConfirmModal from '../common/ConfirmModal';

const PAGE_SIZE = 10;
// AI작성: 화면 필터를 서버 신청 상태에 연결한다. 계금 요청 Mock은 별도 탭에만 유지한다.
const REQUEST_STATUS = { 대기: 'PENDING', 승인: 'APPROVED', 거절: 'REJECTED' };

function SavingGroupJoinRequestsPanel({ savingGroupId }) {
  const [activeFilter, setActiveFilter] = useState('전체');
  const [selectedIds, setSelectedIds] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [payoutCurrentPage, setPayoutCurrentPage] = useState(1);
  const [reviewApplicant, setReviewApplicant] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [result, setResult] = useState(null);
  const [pendingId, setPendingId] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [rejectTarget, setRejectTarget] = useState(null);
  const [notice, setNotice] = useState(null);
  const submitting = useRef(false);
  const active = useRef(false);
  const isPayout = activeFilter === '계금 요청';
  const status = REQUEST_STATUS[activeFilter];
  const queryKey = `${savingGroupId}:${status || 'ALL'}:${currentPage}:${refresh}`;
  const currentResult = result?.key === queryKey ? result : null;
  const isLoading = !isPayout && currentResult === null;
  const requests = currentResult?.items || [];
  const totalPages = currentResult?.meta?.totalPages || 0;
  const errorMessage = currentResult?.error || '';

  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);

  useEffect(() => {
    if (isPayout) return undefined;
    const controller = new AbortController();
    // AI작성: 이전 페이지·필터의 요청을 취소하고 현재 조회 결과만 표시한다.
    getSavingGroupJoinRequests(savingGroupId, { status, page: currentPage, signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setResult({ key: queryKey, ...data });
      })
      .catch((error) => {
        if (!controller.signal.aborted) setResult({ key: queryKey, items: [], error: error.message });
      });
    return () => controller.abort();
  }, [savingGroupId, status, currentPage, queryKey, isPayout]);

  const changeFilter = (filter) => {
    if (submitting.current) return;
    setActiveFilter(filter);
    setCurrentPage(1);
    setPayoutCurrentPage(1);
    setSelectedIds([]);
    setReviewApplicant('');
  };

  const changePage = (page) => {
    if (submitting.current || isLoading) return;
    setCurrentPage(Math.min(Math.max(page, 1), Math.max(totalPages, 1)));
    setSelectedIds([]);
  };

  const retry = () => {
    if (!submitting.current) setRefresh((value) => value + 1);
  };

  const approveRequest = async (requestId) => {
    if (submitting.current || isLoading || errorMessage
      || !requests.some((request) => request.id === requestId && request.status === '대기')) return;
    // AI작성: 상태 반영 전 연속 클릭도 ref로 차단하고 서버 성공 뒤에만 목록을 다시 읽는다.
    submitting.current = true;
    setPendingId(requestId);
    setPendingAction('approve');
    try {
      await approveSavingGroupJoin(savingGroupId, requestId);
      if (active.current) {
        setNotice({ title: '가입 승인 완료', message: '가입 신청을 승인했습니다.' });
        setSelectedIds([]);
        setRefresh((value) => value + 1);
      }
    } catch (error) {
      if (active.current) {
        setNotice({ title: '가입 승인 실패', message: error.message || '승인하지 못했습니다. 다시 확인해주세요.' });
        // AI작성: 충돌·권한 변경 시 오래된 신청 대신 서버 목록을 재확인한다.
        setRefresh((value) => value + 1);
      }
    } finally {
      submitting.current = false;
      if (active.current) {
        setPendingId(null);
        setPendingAction(null);
      }
    }
  };

  // AI작성: 거절 API 호출 전 대기 신청인지 한 번 더 확인하고 확인 모달에 신청자 정보를 전달한다.
  const openRejectConfirm = (requestId) => {
    if (submitting.current || isLoading || errorMessage) return;
    const request = requests.find((item) => item.id === requestId && item.status === '대기');
    if (request) setRejectTarget(request);
  };

  const rejectRequest = async () => {
    const requestId = rejectTarget?.id;
    if (!requestId || submitting.current || isLoading || errorMessage
      || !requests.some((request) => request.id === requestId && request.status === '대기')) return;
    // AI작성: 확인 후에는 모달을 먼저 닫아 같은 거절을 다시 확정하지 못하게 하고 서버 성공 뒤에만 목록을 재조회한다.
    setRejectTarget(null);
    submitting.current = true;
    setPendingId(requestId);
    setPendingAction('reject');
    try {
      await rejectSavingGroupJoin(savingGroupId, requestId);
      if (active.current) {
        setNotice({ title: '가입 거절 완료', message: '가입 신청을 거절했습니다.' });
        setSelectedIds([]);
        setRefresh((value) => value + 1);
      }
    } catch (error) {
      if (active.current) {
        setNotice({ title: '가입 거절 실패', message: error.message || '거절하지 못했습니다. 다시 확인해주세요.' });
        // AI작성: 충돌·권한 변경 시 오래된 신청 대신 서버 목록을 재확인한다.
        setRefresh((value) => value + 1);
      }
    } finally {
      submitting.current = false;
      if (active.current) {
        setPendingId(null);
        setPendingAction(null);
      }
    }
  };

  const toggleRequest = (requestId) => {
    if (pendingId || isLoading || errorMessage) return;
    setSelectedIds((currentIds) => (
      currentIds.includes(requestId) ? currentIds.filter((idValue) => idValue !== requestId) : [...currentIds, requestId]
    ));
  };

  const toggleAll = () => {
    if (pendingId || isLoading || errorMessage) return;
    const pendingIds = requests.filter((request) => request.status === '대기').map((request) => request.id);
    const isAllSelected = pendingIds.length > 0 && pendingIds.every((requestId) => selectedIds.includes(requestId));
    setSelectedIds(isAllSelected ? [] : pendingIds);
  };

  // AI작성: 선택 승인은 API 정책이 확정되지 않아 실제 데이터를 로컬에서 변경하지 않는다.
  const unavailableAction = () => {};
  const payoutTotalPages = Math.max(1, Math.ceil(savingGroupPayoutRequests.length / PAGE_SIZE));
  const visiblePayoutRequests = savingGroupPayoutRequests.slice((payoutCurrentPage - 1) * PAGE_SIZE, payoutCurrentPage * PAGE_SIZE);

  return (
    <>
      <SavingGroupJoinRequestList
        requests={requests}
        activeFilter={activeFilter}
        onFilterChange={changeFilter}
        selectedIds={selectedIds}
        onToggleAll={toggleAll}
        onToggleRequest={toggleRequest}
        onApprove={approveRequest}
        onReject={openRejectConfirm}
        onBulkApprove={unavailableAction}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={changePage}
        totalCount={currentResult?.meta?.total || 0}
        isLoading={isLoading}
        errorMessage={errorMessage}
        pendingId={pendingId}
        pendingAction={pendingAction}
        onRetry={retry}
        onFirstPage={() => changePage(1)}
        payoutRequests={visiblePayoutRequests}
        payoutCurrentPage={payoutCurrentPage}
        payoutTotalPages={payoutTotalPages}
        onPayoutPageChange={(page) => setPayoutCurrentPage(Math.min(Math.max(page, 1), payoutTotalPages))}
        payoutTotalCount={savingGroupPayoutRequests.length}
        onPayoutReview={setReviewApplicant}
        reviewApplicant={reviewApplicant}
      />
      {rejectTarget && (
        <ConfirmModal
          title="가입 신청을 거절할까요?"
          description={`${rejectTarget.applicant}님의 가입 신청은 거절 후 다시 신청할 수 없습니다.`}
          confirmLabel="거절"
          confirmVariant="danger"
          cancelFirst
          onConfirm={rejectRequest}
          onCancel={() => setRejectTarget(null)}
        />
      )}
      {notice && (
        <ConfirmModal title={notice.title} description={notice.message} showCancel={false}
          onConfirm={() => setNotice(null)} onCancel={() => setNotice(null)} />
      )}
    </>
  );
}

export default SavingGroupJoinRequestsPanel;
