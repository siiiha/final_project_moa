import { useMemo, useState } from 'react';
import SavingGroupJoinRequestList from './SavingGroupJoinRequestList';
import { savingGroupJoinRequests } from '../../mocks/savingGroupJoinRequests';
import { savingGroupPayoutRequests } from '../../mocks/savingGroupPayoutRequests';

const PAGE_SIZE = 10;

function SavingGroupJoinRequestsPanel() {
  const [requests, setRequests] = useState(savingGroupJoinRequests);
  const [activeFilter, setActiveFilter] = useState('전체');
  const [selectedIds, setSelectedIds] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [payoutCurrentPage, setPayoutCurrentPage] = useState(1);
  const [reviewApplicant, setReviewApplicant] = useState('');

  const filteredRequests = useMemo(
    () => (activeFilter === '전체' ? requests : requests.filter((request) => request.status === activeFilter)),
    [activeFilter, requests],
  );
  const totalPages = Math.max(1, Math.ceil(filteredRequests.length / PAGE_SIZE));
  const visibleRequests = filteredRequests.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const payoutTotalPages = Math.max(1, Math.ceil(savingGroupPayoutRequests.length / PAGE_SIZE));
  const visiblePayoutRequests = savingGroupPayoutRequests.slice((payoutCurrentPage - 1) * PAGE_SIZE, payoutCurrentPage * PAGE_SIZE);

  const changeFilter = (filter) => {
    setActiveFilter(filter);
    setCurrentPage(1);
    setPayoutCurrentPage(1);
    setSelectedIds([]);
    setReviewApplicant('');
  };

  const changePage = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
    setSelectedIds([]);
  };

  const changeRequestStatus = (requestId, status) => {
    // TODO: Spring Boot 서버에 가입 신청 승인·거절 API 요청을 연결합니다.
    setRequests((currentRequests) => currentRequests.map((request) => (
      request.id === requestId ? { ...request, status } : request
    )));
    setSelectedIds((currentIds) => currentIds.filter((idValue) => idValue !== requestId));
  };

  const toggleRequest = (requestId) => {
    setSelectedIds((currentIds) => (
      currentIds.includes(requestId) ? currentIds.filter((idValue) => idValue !== requestId) : [...currentIds, requestId]
    ));
  };

  const toggleAll = () => {
    const pendingIds = visibleRequests.filter((request) => request.status === '대기').map((request) => request.id);
    const isAllSelected = pendingIds.length > 0 && pendingIds.every((requestId) => selectedIds.includes(requestId));
    setSelectedIds(isAllSelected ? [] : pendingIds);
  };

  const bulkApprove = () => {
    // TODO: Spring Boot 서버에 선택 가입 신청 일괄 승인 API 요청을 연결합니다.
    setRequests((currentRequests) => currentRequests.map((request) => (
      selectedIds.includes(request.id) ? { ...request, status: '승인' } : request
    )));
    setSelectedIds([]);
  };

  const handlePayoutReview = (applicant) => {
    // TODO: Spring Boot 서버에서 계금·지출 요청의 증빙 및 상세 내역을 조회합니다.
    setReviewApplicant(applicant);
  };

  return (
    <SavingGroupJoinRequestList
      requests={visibleRequests}
      activeFilter={activeFilter}
      onFilterChange={changeFilter}
      selectedIds={selectedIds}
      onToggleAll={toggleAll}
      onToggleRequest={toggleRequest}
      onApprove={(requestId) => changeRequestStatus(requestId, '승인')}
      onReject={(requestId) => changeRequestStatus(requestId, '거절')}
      onBulkApprove={bulkApprove}
      currentPage={currentPage}
      totalPages={totalPages}
      onPageChange={changePage}
      totalCount={filteredRequests.length}
      payoutRequests={visiblePayoutRequests}
      payoutCurrentPage={payoutCurrentPage}
      payoutTotalPages={payoutTotalPages}
      onPayoutPageChange={(page) => setPayoutCurrentPage(Math.min(Math.max(page, 1), payoutTotalPages))}
      payoutTotalCount={savingGroupPayoutRequests.length}
      onPayoutReview={handlePayoutReview}
      reviewApplicant={reviewApplicant}
    />
  );
}

export default SavingGroupJoinRequestsPanel;
