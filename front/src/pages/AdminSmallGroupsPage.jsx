import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import SmallGroupFilterBar from '../components/admin/SmallGroupFilterBar';
import SmallGroupTable from '../components/admin/SmallGroupTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import { adminSmallGroups } from '../mocks/adminSmallGroups';

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

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.article`
  min-height: 108px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
`;

const SummaryValue = styled.strong`
  display: block;
  margin-top: 13px;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.text)};
  font-size: 24px;
  letter-spacing: -1px;
`;

const SummaryDescription = styled.p`
  margin-top: 2px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

function AdminSmallGroupsPage() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState(adminSmallGroups);
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredGroups = useMemo(() => {
    const normalizedQuery = submittedQuery.trim().toLowerCase();

    return groups.filter((group) => {
      const matchesStatus = status === '전체' || group.status === status;
      const matchesRegion = region === '전체' || group.region.startsWith(region);
      const matchesQuery = !normalizedQuery || group.name.toLowerCase().includes(normalizedQuery);

      return matchesStatus && matchesRegion && matchesQuery;
    });
  }, [groups, region, status, submittedQuery]);

  const totalPages = Math.max(1, Math.ceil(filteredGroups.length / PAGE_SIZE));
  const pageGroups = filteredGroups.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const activeCount = groups.filter((group) => group.status === '활동중').length;
  const suspendedCount = groups.filter((group) => group.status === '정지').length;

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleStatusAction = (groupId) => {
    // TODO: Spring Boot 소모임 이용 제한·제한 해제 API 요청 후 소모임 목록을 갱신합니다.
    setGroups((currentGroups) =>
      currentGroups.map((group) =>
        group.id === groupId ? { ...group, status: group.status === '활동중' ? '정지' : '활동중' } : group
      )
    );
    setCurrentPage(1);
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

    if (menuId === 'gathering-review') {
      navigate('/admin/saving-groups/applications');
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
        <h1>소모임 관리</h1>
        <p>서비스에서 운영 중인 소모임의 활동 현황과 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="groups" onSelect={handleMenuSelect} />
        <main>
          <SummaryGrid aria-label="소모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 소모임</SummaryLabel>
              <SummaryValue>{groups.length}개</SummaryValue>
              <SummaryDescription>서비스에 등록된 소모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>활동 중</SummaryLabel>
              <SummaryValue>{activeCount}개</SummaryValue>
              <SummaryDescription>정상 운영 중인 소모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>이용 제한</SummaryLabel>
              <SummaryValue $danger>{suspendedCount}개</SummaryValue>
              <SummaryDescription>현재 정지된 소모임</SummaryDescription>
            </SummaryCard>
          </SummaryGrid>
          <SmallGroupTable
            groups={pageGroups}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
            onStatusAction={handleStatusAction}
            filter={
              <SmallGroupFilterBar
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
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminSmallGroupsPage;
