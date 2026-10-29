import { useEffect, useState } from 'react';
import styled from 'styled-components';
import { getSmallGroupJoinRequests } from '../../api/smallGroups';
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

// AI작성: 화면 상태 필터를 서버의 가입 신청 상태값으로만 변환한다.
const REQUEST_STATUS = { 대기: 'PENDING', 승인: 'APPROVED', 거절: 'REJECTED' };

function SmallGroupJoinRequestsPanel({ smallGroupId, groupName }) {
  const [activeFilter, setActiveFilter] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState(null);
  const status = REQUEST_STATUS[activeFilter];
  const queryKey = `${smallGroupId}:${status || 'ALL'}:${currentPage}:${retry}`;
  const currentResult = result?.key === queryKey ? result : null;
  const isLoading = currentResult === null;
  const requests = currentResult?.items || [];
  const totalPages = currentResult?.meta?.totalPages || 0;
  const totalCount = currentResult?.meta?.total || 0;
  const errorMessage = currentResult?.error || '';

  useEffect(() => {
    const controller = new AbortController();
    // AI작성: 필터·페이지·소모임 전환 시 이전 요청을 취소해 다른 모임의 가입 신청 목록이 보이지 않게 한다.
    getSmallGroupJoinRequests(smallGroupId, { status, page: currentPage, signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setResult({ key: queryKey, ...data });
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setResult({ key: queryKey, items: [], meta: null, error: error.message || '가입 신청 목록을 불러오지 못했습니다.' });
        }
      });
    return () => controller.abort();
  }, [smallGroupId, status, currentPage, queryKey]);

  const changeFilter = (filter) => {
    if (isLoading) return;
    setActiveFilter(filter);
    setCurrentPage(1);
  };

  const changePage = (page) => {
    if (isLoading || errorMessage) return;
    setCurrentPage(Math.min(Math.max(page, 1), Math.max(totalPages, 1)));
  };

  return (
    <section>
      <Heading>
        <div>
          <h2>{groupName} 가입 신청 목록</h2>
          <p>가입 메시지와 신청 상태를 확인할 수 있습니다.</p>
        </div>
      </Heading>
      <SmallGroupJoinRequestList
        requests={requests}
        activeFilter={activeFilter}
        onFilterChange={changeFilter}
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={changePage}
        totalCount={totalCount}
        isLoading={isLoading}
        errorMessage={errorMessage}
        onRetry={() => setRetry((value) => value + 1)}
      />
    </section>
  );
}

export default SmallGroupJoinRequestsPanel;
