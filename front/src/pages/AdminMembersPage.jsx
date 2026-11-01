import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminDashboard from '../components/admin/AdminDashboard';
import AdminFilterBar from '../components/admin/AdminFilterBar';
import AdminMemberTable from '../components/admin/AdminMemberTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { getAdminMembers } from '../api/adminMembers';
import { getAdminDashboardSummary } from '../api/adminDashboard';

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
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [restrictionScope, setRestrictionScope] = useState('');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [dashboardStats, setDashboardStats] = useState([]);

  useEffect(() => {
    const controller = new AbortController();

    getAdminDashboardSummary({ signal: controller.signal }).then((summary) => {
      // AI작성: API 집계값을 기존 대시보드 카드 표시 형식으로 변환한다.
      setDashboardStats([
        { id: 'total', label: '전체 회원', value: `${summary.totalMembers.toLocaleString()}명`, description: '탈퇴하지 않은 회원 기준', tone: 'primary' },
        { id: 'active', label: '활성 회원', value: `${summary.activeMembers.toLocaleString()}명`, description: '현재 ACTIVE 계정 기준', tone: 'success' },
        { id: 'new', label: '신규 가입', value: `${summary.newMembersThisMonth.toLocaleString()}명`, description: '이번 달 가입 회원', tone: 'neutral' },
        { id: 'suspended', label: '관리 필요 회원', value: `${summary.managedMembers.toLocaleString()}명`, description: '휴면 및 제재 계정 기준', tone: 'danger' },
      ]);
    }).catch((error) => {
      if (error.code !== 'ERR_CANCELED') setDashboardStats([]);
    });

    return () => controller.abort();
  }, []);

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
          restrictionScope,
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
  }, [currentPage, region, restrictionScope, status, submittedQuery]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
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
        <AdminSidebar items={adminMenuItems} activeItem="members" />
        <main>
          <AdminDashboard stats={dashboardStats} />
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
                restrictionScope={restrictionScope}
                query={query}
                onStatusChange={(value) => {
                  setStatus(value);
                  setCurrentPage(1);
                }}
                onRegionChange={(value) => {
                  setRegion(value);
                  setCurrentPage(1);
                }}
                onRestrictionScopeChange={(value) => {
                  setRestrictionScope(value);
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
