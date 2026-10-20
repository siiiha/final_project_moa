import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SavingGroupFilterBar from '../components/savingGroup/SavingGroupFilterBar';
import SavingGroupList from '../components/savingGroup/SavingGroupList';
import { savingGroupRegions, savingGroups } from '../mocks/savingGroups';

const GROUPS_PER_PAGE = 6;

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 24px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Heading = styled.div`
  margin-top: 8px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -1.2px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const CreateButton = styled(Button)`
  min-height: 38px;
  padding: 8px 14px;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const Content = styled.main`
  margin-top: 18px;
`;

const Result = styled.p`
  margin-top: 18px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  font-weight: 700;

  strong {
    color: ${({ theme }) => theme.colors.primary};
  }
`;

function SavingGroupsPage() {
  const navigate = useNavigate();
  const [region, setRegion] = useState('전체');
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const filteredGroups = useMemo(() => {
    // TODO: Spring Boot 계모임 목록 조회 API에 region, keyword, page 정보를 전달합니다.
    const normalizedKeyword = submittedKeyword.trim().toLowerCase();

    return savingGroups.filter((group) => {
      const matchesRegion = region === '전체' || group.region === region;
      const matchesKeyword =
        !normalizedKeyword ||
        group.name.toLowerCase().includes(normalizedKeyword) ||
        group.description.toLowerCase().includes(normalizedKeyword);

      return matchesRegion && matchesKeyword;
    });
  }, [region, submittedKeyword]);

  const totalPages = Math.max(1, Math.ceil(filteredGroups.length / GROUPS_PER_PAGE));
  const visibleGroups = filteredGroups.slice((currentPage - 1) * GROUPS_PER_PAGE, currentPage * GROUPS_PER_PAGE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedKeyword(keyword);
    setCurrentPage(1);
  };

  const handleCreateApplication = () => {
    navigate('/saving-groups/new');
  };

  return (
    <Page>
      <Breadcrumb>계모임 &gt; 계모임 목록</Breadcrumb>
      <Heading>
        <div>
          <h1>계모임 둘러보기</h1>
          <p>같은 지역 이웃과 함께 목표를 만들고 즐겁게 모아보세요.</p>
        </div>
        <CreateButton type="button" onClick={handleCreateApplication}>
          계모임 개설 신청
        </CreateButton>
      </Heading>
      <Content>
        <SavingGroupFilterBar
          region={region}
          keyword={keyword}
          regions={savingGroupRegions}
          onRegionChange={(value) => {
            setRegion(value);
            setCurrentPage(1);
          }}
          onKeywordChange={setKeyword}
          onSearch={handleSearch}
        />
        <Result>
          총 <strong>{filteredGroups.length}</strong>개의 계모임
        </Result>
        <SavingGroupList
          groups={visibleGroups}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </Content>
    </Page>
  );
}

export default SavingGroupsPage;
