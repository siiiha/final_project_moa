import { useEffect, useState } from 'react';
import styled from 'styled-components';
import SmallGroupFilterBar from '../components/admin/SmallGroupFilterBar';
import SmallGroupTable from '../components/admin/SmallGroupTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import client from '../api/client';
import { smallGroupRegionOptions } from '../api/smallGroups';

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
  const [groups, setGroups] = useState([]);
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadGroups() {
      try {
        const regionCode = smallGroupRegionOptions.find((item) => item.name === region)?.code;
        const response = await client.get('/admin/small-groups', {
          params: {
            keyword: submittedQuery || undefined,
            regionCode,
            page: currentPage,
          },
          signal: controller.signal,
        });

        if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
          throw new Error('소모임 목록 응답을 확인할 수 없습니다.');
        }

        // AI작성: 관리자 전용 목록 응답의 모임장·운영 상태를 화면 표시값으로 변환한다.
        setGroups(response.data.data.map((group) => ({
          id: group.smallGroupId,
          name: group.groupName,
          category: group.likeCategory || '-',
          leader: group.creatorNickname || '-',
          region: group.regionName,
          participants: group.memberCount ?? 0,
          status: group.status === 'ACTIVE' ? '활동중' : group.status === 'RESTRICTED' ? '정지' : '-',
        })));
        setTotalCount(response.data.meta.total || 0);
        setTotalPages(response.data.meta.totalPages || 0);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setGroups([]);
          setTotalCount(0);
          setTotalPages(0);
        }
      }
    }

    loadGroups();
    return () => controller.abort();
  }, [currentPage, region, submittedQuery]);

  const activeCount = 0;
  const suspendedCount = 0;

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleStatusAction = (groupId) => {
    // AI작성: 상태 정보가 API에 없어 이용 제한·해제 기능 연결 전에는 목록 데이터만 조회한다.
    void groupId;
  };

  return (
    <Page>
      <Heading>
        <h1>소모임 관리</h1>
        <p>서비스에서 운영 중인 소모임의 활동 현황과 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="groups" />
        <main>
          <SummaryGrid aria-label="소모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 소모임</SummaryLabel>
              <SummaryValue>{totalCount}개</SummaryValue>
              <SummaryDescription>검색 조건에 해당하는 소모임</SummaryDescription>
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
            groups={groups}
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
