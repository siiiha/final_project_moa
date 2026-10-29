import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminDashboard from '../components/admin/AdminDashboard';
import AdminFilterBar from '../components/admin/AdminFilterBar';
import AdminMemberTable from '../components/admin/AdminMemberTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems, adminSummaryStats } from '../mocks/admin';
import { getAdminMembers } from '../api/adminMembers';

const PAGE_SIZE = 10;

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
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });

  useEffect(() => {
    const controller = new AbortController();
    async function loadMembers() {
      setLoading(true);
      setErrorMessage('');
      try {
        const statusMap = { 정상: 'ACTIVE', 제재: 'SUSPENDED', 탈퇴: 'INACTIVE' };
        const result = await getAdminMembers({
          keyword: submittedQuery,
          regionName: region === '전체' ? '' : region,
          accountStatus: statusMap[status] || '',
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setMembers(result.items);
        setPageMeta(result.meta);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setMembers([]);
          setErrorMessage(error.message);
          setPageMeta({ total: 0, totalPages: 0 });
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    loadMembers();
    return () => controller.abort();
  }, [currentPage, region, status, submittedQuery]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
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
            members={members}
            loading={loading}
            errorMessage={errorMessage}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            filter={
              <AdminFilterBar
                status={status}
                region={region}
                query={query}
                onStatusChange={(value) => {
                  setStatus(value);
                  setCurrentPage(1);
                }}
                onRegionChange={(value) => {
                  setRegion(value);
                  setCurrentPage(1);
                }}
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
