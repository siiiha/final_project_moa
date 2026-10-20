import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminReportFilterBar from '../components/admin/AdminReportFilterBar';
import AdminReportTable from '../components/admin/AdminReportTable';
import Badge from '../components/common/Badge';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminReports } from '../mocks/adminReports';

const PAGE_SIZE = 10;

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

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

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

const AdminBadge = styled(Badge)`
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.border};
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

const Content = styled.main`
  padding-top: 4px;
`;

function AdminReportsPage() {
  const navigate = useNavigate();
  // TODO: Spring Boot 관리자 회원 신고 목록 조회 API로 adminReports mock 데이터를 대체합니다.
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [reason, setReason] = useState('전체');
  const [status, setStatus] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredReports = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return adminReports.filter((report) => {
      const matchesQuery =
        !normalizedQuery ||
        report.target.toLowerCase().includes(normalizedQuery) ||
        report.reporter.toLowerCase().includes(normalizedQuery);
      const matchesReason = reason === '전체' || report.reason === reason;
      const matchesStatus = status === '전체' || report.status === status;

      return matchesQuery && matchesReason && matchesStatus;
    });
  }, [reason, status, submittedQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredReports.length / PAGE_SIZE));
  const pageReports = filteredReports.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleMenuSelect = (menuId) => {
    const routes = {
      members: '/admin/members',
      gatherings: '/admin/saving-groups',
      'gathering-review': '/admin/saving-groups/applications',
      groups: '/admin/small-groups',
      posts: '/admin/posts',
      comments: '/admin/comments',
      reports: '/admin/reposts',
    };

    if (routes[menuId]) {
      navigate(routes[menuId]);
      return;
    }
  };

  return (
    <Page>
      <Heading>
        <div>
          <h1>회원 신고</h1>
          <p>접수된 회원 신고 내역을 조회하고 처리 현황을 확인하세요.</p>
        </div>
        <AdminBadge>관리자 전용</AdminBadge>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="reports" onSelect={handleMenuSelect} />
        <Content>
          <AdminReportFilterBar
            query={query}
            reason={reason}
            status={status}
            onQueryChange={setQuery}
            onReasonChange={(value) => {
              setReason(value);
              setCurrentPage(1);
            }}
            onStatusChange={(value) => {
              setStatus(value);
              setCurrentPage(1);
            }}
            onSearch={handleSearch}
          />
          <AdminReportTable
            reports={pageReports}
            totalCount={filteredReports.length}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
            onViewReport={(reportId) => navigate(`/admin/reposts/${reportId}`)}
          />
        </Content>
      </Grid>
    </Page>
  );
}

export default AdminReportsPage;
