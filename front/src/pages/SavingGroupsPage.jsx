import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SavingGroupFilterBar from '../components/savingGroup/SavingGroupFilterBar';
import SavingGroupList from '../components/savingGroup/SavingGroupList';
import { getSavingGroups } from '../api/savingGroups';
import { getMemberRegions } from '../api/member';

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
  // AI작성: 목록 주소에서 적용된 검색 조건과 페이지를 복원한다.
  const [params, setParams] = useSearchParams();
  const submittedKeyword = (params.get('keyword') ?? '').trim().slice(0, 20);
  const province = params.get('province') ?? '';
  const districtId = province ? params.get('districtId') ?? '' : '';
  const townId = districtId ? params.get('townId') ?? '' : '';
  const page = Number(params.get('page'));
  const currentPage = Number.isSafeInteger(page) && page > 0 ? page : 1;
  const updateQuery = (changes, nextPage = 1) => {
    const next = new URLSearchParams(params);
    Object.entries({ ...changes, page: nextPage > 1 ? nextPage : '' }).forEach(([key, value]) => {
      if (value) next.set(key, value);
      else next.delete(key);
    });
    setParams(next);
  };
  // AI작성: 회원가입과 같은 지역 목록을 사용하며 구·동은 필수가 아니다.
  const [regionState, setRegionState] = useState({ items: [], loading: true, error: '' });
  const [regionRetry, setRegionRetry] = useState(0);
  const [keyword, setKeyword] = useState(submittedKeyword);
  useEffect(() => { setKeyword(submittedKeyword); }, [submittedKeyword]);

  const [result, setResult] = useState({ items: [], totalElements: 0, totalPages: 0, requestKey: null, error: '' });
  const [searchError, setSearchError] = useState('');
  const [retry, setRetry] = useState(0);
  // AI작성: ID는 선택 UI에서만 사용하고 검색은 정규 지역명으로 상위 범위를 지정한다.
  const selectedDistrict = regionState.items.find((item) => item.regionId === Number(districtId));
  const selectedTown = regionState.items.find((item) => item.regionId === Number(townId));
  const districtName = districtId ? selectedDistrict?.districtName : undefined;
  const townName = townId ? selectedTown?.townName : undefined;
  const requestKey = JSON.stringify([submittedKeyword, province, districtName, townName, currentPage, retry]);
  const loading = result.requestKey !== requestKey;
  const error = result.error;

  // AI작성: 화면 이탈·재시도 뒤 도착한 지역 응답은 무시한다.
  useEffect(() => {
    let active = true;
    getMemberRegions().then((items) => {
      if (!Array.isArray(items) || items.length === 0) throw new Error('지역 목록이 비어 있습니다.');
      if (active) setRegionState({ items, loading: false, error: '' });
    }).catch(() => {
      if (active) setRegionState({ items: [], loading: false, error: '지역을 불러오지 못했습니다. 다시 시도해 주세요.' });
    });
    return () => { active = false; };
  }, [regionRetry]);

  // AI작성: 조건 변경·화면 이탈 시 요청을 취소하고 오래된 성공·실패 결과를 무시한다.
  useEffect(() => {
    if ((districtId || townId) && regionState.loading) return;
    const controller = new AbortController();
    let active = true;
    getSavingGroups({
      keyword: submittedKeyword,
      provinceName: province || undefined,
      districtName,
      townName,
      page: currentPage, signal: controller.signal,
    }).then((data) => {
      if (active) setResult({ ...data, requestKey, error: '' });
    }).catch((failure) => {
      if (active) setResult({ items: [], totalElements: 0, totalPages: 0, requestKey,
        error: failure.response?.data?.error?.message
          || '목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' });
    });
    return () => { active = false; controller.abort(); };
  }, [submittedKeyword, province, districtName, townName, currentPage, retry, requestKey, districtId, townId, regionState.loading]);

  // AI작성: 서버와 동일하게 앞뒤 공백 제거 후 길이를 검사한다.
  const handleSearch = (event) => {
    event.preventDefault();
    const normalizedKeyword = keyword.trim();
    if (normalizedKeyword.length > 20) {
      setSearchError('검색어는 20자 이내로 입력해 주세요.');
      return;
    }
    setSearchError('');
    updateQuery({ keyword: normalizedKeyword });
  };

  const handleCreateApplication = () => {
    navigate('/saving-groups/new');
  };

  // AI작성: 빈 결과에서 검색어·지역·페이지를 초기화하고 전체 목록을 다시 조회한다.
  const handleResetSearch = () => {
    setKeyword('');
    setParams({});
    setSearchError('');
    setRetry((value) => value + 1);
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
          province={province}
          districtId={districtId}
          townId={townId}
          keyword={keyword}
          regions={regionState.items}
          regionsLoading={regionState.loading}
          regionsError={regionState.error}
          onProvinceChange={(value) => {
            // AI작성: 상위 지역 변경 시 이전 하위 선택을 남기지 않는다.
            updateQuery({ province: value, districtId: '', townId: '' });
          }}
          onDistrictChange={(value) => {
            updateQuery({ districtId: value, townId: '' });
          }}
          onTownChange={(value) => {
            updateQuery({ townId: value });
          }}
          onKeywordChange={setKeyword}
          onSearch={handleSearch}
        />
        {regionState.loading && <Result role="status">지역을 불러오는 중입니다.</Result>}
        {regionState.error && (
          <div role="alert">
            <Result>{regionState.error}</Result>
            <Button type="button" onClick={() => {
              setRegionState({ items: [], loading: true, error: '' });
              setRegionRetry((value) => value + 1);
            }}>지역 다시 불러오기</Button>
          </div>
        )}
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
              onPageChange={(nextPage) => updateQuery({}, nextPage)}
            />
            {/* AI작성: 범위 초과는 기존 첫 페이지 이동을 사용하고 정상 빈 결과에만 초기화를 제공한다. */}
            {result.items.length === 0 && result.totalPages === 0 && (
              <Button type="button" $variant="secondary" onClick={handleResetSearch}>검색 조건 초기화</Button>
            )}
          </>
        )}
      </Content>
    </Page>
  );
}

export default SavingGroupsPage;
