import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupApplicationFilterBar from '../components/admin/SavingGroupApplicationFilterBar';
import SavingGroupApplicationTable from '../components/admin/SavingGroupApplicationTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { getSavingGroupCreationApplications } from '../api/savingGroups';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

function AdminSavingGroupApplicationsPage() {
  const navigate = useNavigate();
  // AI작성: 빈 문자열은 전체 조회, 나머지는 백엔드 검증 코드로 유지해 별도 변환 없이 API에 전달한다.
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [result, setResult] = useState({ items: [], meta: null, requestKey: null, error: '' });
  const [retry, setRetry] = useState(0);
  const requestKey = JSON.stringify([type, status, submittedQuery, currentPage, retry]);
  const loading = result.requestKey !== requestKey;

  // AI작성: 필터·페이지별 요청을 취소해 늦게 도착한 이전 응답이 화면을 덮어쓰지 않게 한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSavingGroupCreationApplications({
      status: status || undefined,
      type: type || undefined,
      keyword: submittedQuery,
      page: currentPage,
      signal: controller.signal,
    })
      .then((data) => {
        if (active) setResult({ ...data, requestKey, error: '' });
      })
      .catch((failure) => {
        if (active && failure.code !== 'ERR_CANCELED') {
          setResult({ items: [], meta: null, requestKey, error: failure.message });
        }
      });
    return () => {
      active = false;
      controller.abort();
    };
  }, [type, status, submittedQuery, currentPage, retry, requestKey]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleReview = (applicationId) => {
    navigate(`/admin/saving-groups/applications/${applicationId}`);
  };

  return (
    <Page>
      <Heading>
        <h1>계모임 심사 목록</h1>
        <p>계모임 개설·삭제 신청 내역을 확인하고 심사 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gathering-review" />
        <main>
          {loading ? (
            <p role="status">심사 목록을 불러오는 중입니다.</p>
          ) : result.error ? (
            <div role="alert">
              <p>{result.error}</p>
              <button type="button" onClick={() => setRetry((value) => value + 1)}>
                다시 시도
              </button>
            </div>
          ) : (
            <SavingGroupApplicationTable
              applications={result.items}
              currentPage={currentPage}
              totalPages={result.meta?.totalPages || 0}
              onPageChange={setCurrentPage}
              onReview={handleReview}
              filter={
                <SavingGroupApplicationFilterBar
                  status={status}
                  type={type}
                  query={query}
                  onStatusChange={(value) => {
                    setStatus(value);
                    setCurrentPage(1);
                  }}
                  onTypeChange={(value) => {
                    setType(value);
                    setCurrentPage(1);
                  }}
                  onQueryChange={setQuery}
                  onSearch={handleSearch}
                />
              }
            />
          )}
        </main>
      </Grid>
    </Page>
  );
}

export default AdminSavingGroupApplicationsPage;
