import { useMemo, useState } from 'react';
import { FiPlus } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SmallGroupFilterBar from '../components/smallGroup/SmallGroupFilterBar';
import SmallGroupList from '../components/smallGroup/SmallGroupList';
import { smallGroupInterests, smallGroupRegions, smallGroups } from '../mocks/smallGroups';

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

const ListHeader = styled.div`
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
`;

const Result = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  font-weight: 700;

  strong {
    color: ${({ theme }) => theme.colors.primary};
  }
`;

const SortField = styled.label`
  display: flex;
  align-items: center;
  gap: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const SortSelect = styled.select`
  width: 110px;
  height: 32px;
  padding: 0 9px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Notice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
`;

function SmallGroupsPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [region, setRegion] = useState('서울 마포구');
  const [interest, setInterest] = useState('전체');
  const [sort, setSort] = useState('latest');
  const [currentPage, setCurrentPage] = useState(1);
  const [notice, setNotice] = useState('');

  const filteredGroups = useMemo(() => {
    // TODO: Spring Boot 서버의 소모임 목록 API에 검색어, 지역, 관심사, 정렬 조건을 전달합니다.
    const normalizedKeyword = submittedKeyword.trim().toLowerCase();
    const filtered = smallGroups.filter((group) => {
      const matchesKeyword = !normalizedKeyword || group.name.toLowerCase().includes(normalizedKeyword) || group.description.toLowerCase().includes(normalizedKeyword);
      const matchesRegion = region === '전체' || group.region === region;
      const matchesInterest = interest === '전체' || group.interest === interest;
      return matchesKeyword && matchesRegion && matchesInterest;
    });

    return [...filtered].sort((first, second) => (
      sort === 'latest'
        ? second.createdAt.localeCompare(first.createdAt)
        : first.name.localeCompare(second.name, 'ko')
    ));
  }, [interest, region, sort, submittedKeyword]);
  const totalPages = Math.max(1, Math.ceil(filteredGroups.length / GROUPS_PER_PAGE));
  const visibleGroups = filteredGroups.slice((currentPage - 1) * GROUPS_PER_PAGE, currentPage * GROUPS_PER_PAGE);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedKeyword(keyword);
    setCurrentPage(1);
    setNotice('');
  };

  const handleCreate = () => {
    navigate('/small-groups/new');
  };

  const handleView = (id) => {
    navigate(`/small-groups/${id}`);
  };

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록</Breadcrumb>
      <Heading>
        <div>
          <h1>우리 동네 소모임</h1>
          <p>취미가 같은 이웃과 즐거운 일상을 함께해요.</p>
        </div>
        <CreateButton type="button" onClick={handleCreate}>
          <FiPlus size={15} aria-hidden="true" /> 소모임 만들기
        </CreateButton>
      </Heading>

      <Content>
        <SmallGroupFilterBar
          keyword={keyword}
          region={region}
          interest={interest}
          regions={smallGroupRegions}
          interests={smallGroupInterests}
          onKeywordChange={setKeyword}
          onRegionChange={(value) => {
            setRegion(value);
            setCurrentPage(1);
          }}
          onInterestChange={(value) => {
            setInterest(value);
            setCurrentPage(1);
          }}
          onSearch={handleSearch}
        />

        <ListHeader>
          <Result>검색 결과 <strong>{filteredGroups.length}</strong>개</Result>
          <SortField>
            정렬
            <SortSelect
              value={sort}
              onChange={(event) => {
                setSort(event.target.value);
                setCurrentPage(1);
              }}
              aria-label="소모임 정렬"
            >
              <option value="latest">최신순</option>
              <option value="name">이름순</option>
            </SortSelect>
          </SortField>
        </ListHeader>

        <SmallGroupList
          groups={visibleGroups}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={(page) => setCurrentPage(Math.min(Math.max(page, 1), totalPages))}
          onView={handleView}
        />
        {notice && <Notice role="status">{notice}</Notice>}
      </Content>
    </Page>
  );
}

export default SmallGroupsPage;
