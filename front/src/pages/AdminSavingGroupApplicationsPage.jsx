import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupApplicationFilterBar from '../components/admin/SavingGroupApplicationFilterBar';
import SavingGroupApplicationTable from '../components/admin/SavingGroupApplicationTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminSavingGroupApplications } from '../mocks/adminSavingGroupApplications';

const PAGE_SIZE = 8;

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
  const [status, setStatus] = useState('전체');
  const [reviewType, setReviewType] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredApplications = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return adminSavingGroupApplications.filter((application) => {
      const matchesStatus = status === '전체' || application.status === status;
      const matchesReviewType = reviewType === '전체' || application.reviewType === reviewType;
      const matchesQuery = !normalizedQuery || application.name.toLowerCase().includes(normalizedQuery);

      return matchesStatus && matchesReviewType && matchesQuery;
    });
  }, [reviewType, status, submittedQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredApplications.length / PAGE_SIZE));
  const pageApplications = filteredApplications.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleReview = (applicationId) => {
    // TODO: Spring Boot 계모임 개설 신청 상세 조회 API를 연결합니다.
    navigate(`/admin/saving-groups/applications/${applicationId}`);
  };

  const handleMenuSelect = (menuId) => {
    if (menuId === 'members') {
      navigate('/admin/members');
      return;
    }

    if (menuId === 'gatherings') {
      navigate('/admin/saving-groups');
      return;
    }

    if (menuId === 'groups') {
      navigate('/admin/small-groups');
      return;
    }

    if (menuId === 'posts') {
      navigate('/admin/posts');
      return;
    }

    if (menuId === 'comments') {
      navigate('/admin/comments');
      return;
    }

    if (menuId === 'reports') {
      navigate('/admin/reposts');
      return;
    }

    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  return (
    <Page>
      <Heading>
        <h1>계모임 심사 목록</h1>
        <p>계모임 개설·삭제 신청 내역을 확인하고 심사 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gathering-review" onSelect={handleMenuSelect} />
        <main>
          <SavingGroupApplicationTable
            applications={pageApplications}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
            onReview={handleReview}
            filter={
              <SavingGroupApplicationFilterBar
                status={status}
                reviewType={reviewType}
                query={query}
                onStatusChange={(value) => {
                  setStatus(value);
                  setCurrentPage(1);
                }}
                onReviewTypeChange={(value) => {
                  setReviewType(value);
                  setCurrentPage(1);
                }}
                onQueryChange={setQuery}
                onSearch={handleSearch}
              />
            }
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminSavingGroupApplicationsPage;
