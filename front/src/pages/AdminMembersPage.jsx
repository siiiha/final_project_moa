import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminDashboard from '../components/admin/AdminDashboard';
import AdminFilterBar from '../components/admin/AdminFilterBar';
import AdminMemberTable from '../components/admin/AdminMemberTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMembers, adminMenuItems, adminSummaryStats } from '../mocks/admin';

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

function AdminMembersPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('members');
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');

  const filteredMembers = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return adminMembers.filter((member) => {
      const matchesStatus = status === '전체' || member.status === status;
      const matchesRegion = region === '전체' || member.region.startsWith(region);
      const matchesQuery =
        !normalizedQuery ||
        member.name.toLowerCase().includes(normalizedQuery) ||
        member.email.toLowerCase().includes(normalizedQuery);

      return matchesStatus && matchesRegion && matchesQuery;
    });
  }, [region, status, submittedQuery]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
  };

  const handleMenuSelect = (menuId) => {
    if (menuId === 'gatherings') {
      navigate('/admin/saving-groups');
      return;
    }

    if (menuId === 'gathering-review') {
      navigate('/admin/saving-groups/applications');
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

    setActiveMenu(menuId);
    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  const handleViewMember = (memberId) => {
    navigate(`/admin/members/${memberId}`);
  };

  return (
    <Page>
      <Heading>
        <h1>관리자 대시보드</h1>
        <p>서비스 회원 현황을 확인하고 회원 정보를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem={activeMenu} onSelect={handleMenuSelect} />
        <main>
          <AdminDashboard stats={adminSummaryStats} />
          <AdminMemberTable
            members={filteredMembers}
            filter={
              <AdminFilterBar
                status={status}
                region={region}
                query={query}
                onStatusChange={setStatus}
                onRegionChange={setRegion}
                onQueryChange={setQuery}
                onSearch={handleSearch}
              />
            }
            onViewMember={handleViewMember}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminMembersPage;
