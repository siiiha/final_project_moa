import { useMemo, useState } from 'react';
import styled from 'styled-components';
import { smallGroupJoinRequests } from '../../mocks/smallGroupJoinRequests';
import SmallGroupJoinRequestList from './SmallGroupJoinRequestList';

const Heading = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h2 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 18px;
    font-weight: 800;
    letter-spacing: -0.8px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }
`;

const PAGE_SIZE = 10;

function SmallGroupJoinRequestsPanel({ groupName }) {
  const [requests, setRequests] = useState(smallGroupJoinRequests);
  const [activeFilter, setActiveFilter] = useState('전체');
  const [selectedIds, setSelectedIds] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);

  const filteredRequests = useMemo(
    () => (activeFilter === '전체' ? requests : requests.filter((request) => request.status === activeFilter)),
    [activeFilter, requests],
  );
  const totalPages = Math.max(1, Math.ceil(filteredRequests.length / PAGE_SIZE));
  const visibleRequests = filteredRequests.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const changeFilter = (filter) => {
    setActiveFilter(filter);
    setCurrentPage(1);
    setSelectedIds([]);
  };

  const changePage = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
    setSelectedIds([]);
  };

  const changeStatus = (requestId, status) => {
    // TODO: Spring Boot 소모임 가입 신청 승인·거절 API 요청을 연결합니다.
    setRequests((currentRequests) => currentRequests.map((request) => (
      request.id === requestId ? { ...request, status } : request
    )));
    setSelectedIds((currentIds) => currentIds.filter((id) => id !== requestId));
  };

  const toggleRequest = (requestId) => {
    setSelectedIds((currentIds) => (
      currentIds.includes(requestId) ? currentIds.filter((id) => id !== requestId) : [...currentIds, requestId]
    ));
  };

  const toggleAll = () => {
    const pendingIds = visibleRequests.filter((request) => request.status === '대기').map((request) => request.id);
    const isAllSelected = pendingIds.length > 0 && pendingIds.every((id) => selectedIds.includes(id));
    setSelectedIds(isAllSelected ? [] : pendingIds);
  };

  const bulkApprove = () => {
    // TODO: Spring Boot 소모임 가입 신청 일괄 승인 API 요청을 연결합니다.
    setRequests((currentRequests) => currentRequests.map((request) => (
      selectedIds.includes(request.id) ? { ...request, status: '승인' } : request
    )));
    setSelectedIds([]);
  };

  return (
    <section>
      <Heading>
        <div>
          <h2>{groupName} 가입 신청 목록</h2>
          <p>가입 메시지를 확인한 후 신청을 처리해주세요.</p>
        </div>
      </Heading>
      <SmallGroupJoinRequestList
        requests={visibleRequests}
        activeFilter={activeFilter}
        onFilterChange={changeFilter}
        selectedIds={selectedIds}
        onToggleAll={toggleAll}
        onToggleRequest={toggleRequest}
        onApprove={(requestId) => changeStatus(requestId, '승인')}
        onReject={(requestId) => changeStatus(requestId, '거절')}
        onBulkApprove={bulkApprove}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={changePage}
        totalCount={filteredRequests.length}
      />
    </section>
  );
}

export default SmallGroupJoinRequestsPanel;
