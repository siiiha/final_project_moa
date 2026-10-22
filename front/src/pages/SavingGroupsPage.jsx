import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SavingGroupFilterBar from '../components/savingGroup/SavingGroupFilterBar';
import SavingGroupList from '../components/savingGroup/SavingGroupList';
import { getSavingGroups, savingGroupRegionOptions } from '../api/savingGroups';

// AI작성: 목록 화면의 지역 선택은 실제 API 코드표를 사용한다.
const savingGroupRegions = ['전체', ...savingGroupRegionOptions.map((item) => item.name)];

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

// AI작성: 검색 조건과 서버 페이징을 연결하고 이전 요청의 응답을 폐기한다.
function SavingGroupsPage() {
  const navigate = useNavigate();
  const [region, setRegion] = useState('전체');
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const [result, setResult] = useState({ items: [], totalElements: 0, totalPages: 0, requestKey: null, error: '' });
  const [searchError, setSearchError] = useState('');
  const [retry, setRetry] = useState(0);
  const requestKey = JSON.stringify([region, submittedKeyword, currentPage, retry]);
  const loading = result.requestKey !== requestKey;
  const error = result.error;

  // AI작성: 조건 변경·화면 이탈 시 요청을 취소하고 오래된 성공·실패 결과를 무시한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSavingGroups({
      keyword: submittedKeyword,
      regionCode: savingGroupRegionOptions.find((item) => item.name === region)?.code,
      page: currentPage, signal: controller.signal,
    }).then((data) => {
      if (active) setResult({ ...data, requestKey, error: '' });
    }).catch((failure) => {
      if (active) setResult({ items: [], totalElements: 0, totalPages: 0, requestKey,
        error: failure.response?.data?.error?.message
          || '목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' });
    });
    return () => { active = false; controller.abort(); };
  }, [region, submittedKeyword, currentPage, retry, requestKey]);

  // AI작성: 서버와 동일하게 앞뒤 공백 제거 후 길이를 검사한다.
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
        {searchError && <Result role="alert">{searchError}</Result>}
        {loading ? <Result role="status">계모임 목록을 불러오는 중입니다.</Result> : error ? (
          <div role="alert">
            <Result>{error}</Result>
            <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
          </div>
        ) : (
          <>
            <Result>총 <strong>{result.totalElements}</strong>개의 계모임</Result>
            <SavingGroupList
              groups={result.items}
              currentPage={currentPage}
              totalPages={result.totalPages}
              onPageChange={setCurrentPage}
            />
          </>
        )}
      </Content>
    </Page>
  );
}

export default SavingGroupsPage;
