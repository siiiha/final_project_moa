import { useEffect, useState } from 'react';
import { FiPlus } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SmallGroupFilterBar from '../components/smallGroup/SmallGroupFilterBar';
import SmallGroupList from '../components/smallGroup/SmallGroupList';
import { fetchSmallGroupCategories, getSmallGroups, smallGroupRegionOptions } from '../api/smallGroups';

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

function SmallGroupsPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [region, setRegion] = useState('전체');
  const [interest, setInterest] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState({ items: [], totalElements: 0, totalPages: 0, requestKey: null, error: '' });
  const [categoryState, setCategoryState] = useState({ items: [], loading: true, error: '' });
  const [categoryRetry, setCategoryRetry] = useState(0);
  const [searchError, setSearchError] = useState('');
  const regions = ['전체', ...smallGroupRegionOptions.map((item) => item.name)];
  const interests = ['전체', ...categoryState.items.map((item) => item.interestName)];
  const requestKey = JSON.stringify([submittedKeyword, region, interest, currentPage, retry]);
  const loading = result.requestKey !== requestKey;

  // AI작성: 조건 변경·화면 이탈 시 목록 요청을 취소해 오래된 결과가 현재 화면을 덮지 않게 한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSmallGroups({
      keyword: submittedKeyword,
      regionCode: smallGroupRegionOptions.find((item) => item.name === region)?.code,
      likeCategory: interest === '전체' ? undefined : interest,
      page: currentPage,
      signal: controller.signal,
    }).then((data) => {
      if (active) setResult({ ...data, requestKey, error: '' });
    }).catch((error) => {
      if (active) setResult({ items: [], totalElements: 0, totalPages: 0, requestKey,
        error: error.message || '소모임 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' });
    });
    return () => { active = false; controller.abort(); };
  }, [currentPage, interest, region, requestKey, submittedKeyword]);

  // AI작성: 관심사 선택지는 목록 데이터가 아닌 interest 기준 API에서 별도로 조회한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    fetchSmallGroupCategories({ signal: controller.signal }).then((items) => {
      if (active) setCategoryState({ items, loading: false, error: '' });
    }).catch((error) => {
      if (active) setCategoryState({ items: [], loading: false,
        error: error.message || '관심사 목록을 불러오지 못했습니다.' });
    });
    return () => { active = false; controller.abort(); };
  }, [categoryRetry]);

  const handleSearch = (event) => {
    event.preventDefault();
    const normalizedKeyword = keyword.trim();
    if (normalizedKeyword.length > 20) {
      setSearchError('검색어는 20자 이내로 입력해 주세요.');
      return;
    }
    setSearchError('');
    setSubmittedKeyword(normalizedKeyword);
    setCurrentPage(1);
  };

  const handleCreate = () => {
    navigate('/small-groups/new');
  };

  const handleCategoryRetry = () => {
    setCategoryState((previous) => ({ ...previous, loading: true, error: '' }));
    setCategoryRetry((value) => value + 1);
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
          regions={regions}
          interests={interests}
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

        {categoryState.loading && <Result role="status">관심사 목록을 불러오는 중입니다.</Result>}
        {categoryState.error && (
          <div role="alert">
            <Result>{categoryState.error}</Result>
            <Button type="button" onClick={handleCategoryRetry}>다시 시도</Button>
          </div>
        )}
        {searchError && <Result role="alert">{searchError}</Result>}

        <ListHeader>
          <Result>검색 결과 <strong>{result.totalElements}</strong>개</Result>
          <Result>최신순</Result>
        </ListHeader>

        {loading ? <Result role="status">소모임 목록을 불러오는 중입니다.</Result> : result.error ? (
          <div role="alert">
            <Result>{result.error}</Result>
            <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
          </div>
        ) : (
          <SmallGroupList
            groups={result.items}
            currentPage={currentPage}
            totalPages={result.totalPages}
            onPageChange={setCurrentPage}
            onView={handleView}
          />
        )}
      </Content>
    </Page>
  );
}

export default SmallGroupsPage;
