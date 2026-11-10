import { useEffect, useState } from 'react';
import { FiPlus } from 'react-icons/fi';
import useAuthStore from '../store/authStore';
import { useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import SmallGroupFilterBar from '../components/smallGroup/SmallGroupFilterBar';
import SmallGroupList from '../components/smallGroup/SmallGroupList';
import { fetchSmallGroupCategories, getSmallGroups } from '../api/smallGroups';
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
  // AI작성: 목록 주소에서 적용된 검색 조건과 페이지를 복원한다.
  const token = useAuthStore((state) => state.token);
  const [params, setParams] = useSearchParams();
  const submittedKeyword = (params.get('keyword') ?? '').trim().slice(0, 20);
  const province = params.get('province') ?? '';
  const districtId = province ? params.get('districtId') ?? '' : '';
  const townId = districtId ? params.get('townId') ?? '' : '';
  const page = Number(params.get('page'));
  const currentPage = Number.isSafeInteger(page) && page > 0 ? page : 1;
  const interest = params.get('interest') || '전체';
  const updateQuery = (changes, nextPage = 1) => {
    const next = new URLSearchParams(params);
    Object.entries({ ...changes, page: nextPage > 1 ? nextPage : '' }).forEach(([key, value]) => {
      if (value) next.set(key, value);
      else next.delete(key);
    });
    setParams(next);
  };
  const [keyword, setKeyword] = useState(submittedKeyword);
  useEffect(() => { setKeyword(submittedKeyword); }, [submittedKeyword]);
  // AI작성: 구·동은 선택 사항이며 회원가입·지역 인증의 조회 함수를 재사용한다.
  const [regionState, setRegionState] = useState({ items: [], loading: true, error: '' });
  const [regionRetry, setRegionRetry] = useState(0);
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState({ items: [], totalElements: 0, totalPages: 0, requestKey: null, error: '' });
  const [categoryState, setCategoryState] = useState({ items: [], loading: true, error: '' });
  const [categoryRetry, setCategoryRetry] = useState(0);
  const [searchError, setSearchError] = useState('');
  const selectedDistrict = regionState.items.find((item) => item.regionId === Number(districtId));
  const selectedTown = regionState.items.find((item) => item.regionId === Number(townId));
  const districtName = districtId ? selectedDistrict?.districtName : undefined;
  const townName = townId ? selectedTown?.townName : undefined;
  const interests = ['전체', ...categoryState.items.map((item) => item.interestName)];
  const requestKey = JSON.stringify([submittedKeyword, province, districtName, townName, interest, currentPage, retry]);
  const loading = result.requestKey !== requestKey;

  // AI작성: 화면 이탈·재시도 뒤 도착한 지역 응답이 현재 상태를 덮지 않게 한다.
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

  // AI작성: 조건 변경·화면 이탈 시 목록 요청을 취소해 오래된 결과가 현재 화면을 덮지 않게 한다.
  useEffect(() => {
    if ((districtId || townId) && regionState.loading) return;
    const controller = new AbortController();
    let active = true;
    getSmallGroups({
      keyword: submittedKeyword,
      provinceName: province || undefined,
      districtName,
      townName,
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
  }, [currentPage, interest, province, districtName, townName, requestKey, submittedKeyword, districtId, townId, regionState.loading]);

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
    updateQuery({ keyword: normalizedKeyword });
  };

  const handleCreate = () => {
    navigate('/small-groups/new');
  };

  // AI작성: 빈 결과에서 검색어·지역·관심사·페이지를 초기화하고 전체 목록을 다시 조회한다.
  const handleResetSearch = () => {
    setKeyword('');
    setParams({});
    setSearchError('');
    setRetry((value) => value + 1);
  };

  const handleCategoryRetry = () => {
    setCategoryState((previous) => ({ ...previous, loading: true, error: '' }));
    setCategoryRetry((value) => value + 1);
  };

  const handleView = (id) => {
    // AI작성: 상세의 공지 쿼리와 별도로 목록 복귀 조건을 전달한다.
    navigate({ pathname: `/small-groups/${id}`, search: params.size
      ? new URLSearchParams({ listSearch: params.toString() }).toString() : '' });
  };

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록</Breadcrumb>
      <Heading>
        <div>
          <h1>우리 동네 소모임</h1>
          <p>취미가 같은 이웃과 즐거운 일상을 함께해요.</p>
        </div>
        {/* AI작성: 소모임 만들기 버튼은 로그인한 사용자에게만 표시한다. */}
        {token && (
          <CreateButton type="button" onClick={handleCreate}>
            <FiPlus size={15} aria-hidden="true" /> 소모임 만들기
          </CreateButton>
        )}
      </Heading>

      <Content>
        <SmallGroupFilterBar
          keyword={keyword}
          province={province}
          districtId={districtId}
          townId={townId}
          interest={interest}
          regions={regionState.items}
          regionsLoading={regionState.loading}
          regionsError={regionState.error}
          interests={interests}
          onKeywordChange={setKeyword}
          onProvinceChange={(value) => {
            // AI작성: 상위 변경 시 이전 하위 지역을 초기화하고 첫 페이지로 이동한다.
            updateQuery({ province: value, districtId: '', townId: '' });
          }}
          onDistrictChange={(value) => {
            updateQuery({ districtId: value, townId: '' });
          }}
          onTownChange={(value) => {
            updateQuery({ townId: value });
          }}
          onInterestChange={(value) => {
            updateQuery({ interest: value === '전체' ? '' : value });
          }}
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
        {categoryState.error && (
          <div role="alert">
            <Result>{categoryState.error}</Result>
            <Button type="button" onClick={handleCategoryRetry}>다시 시도</Button>
          </div>
        )}
        {searchError && <Result role="alert">{searchError}</Result>}

        {/* AI작성: 목록 로딩·실패 중 이전 결과 건수나 오류에 의한 0건을 표시하지 않는다. */}
        {!loading && !result.error && <ListHeader>
          <Result>검색 결과 <strong>{result.totalElements}</strong>개</Result>
          <Result>최신순</Result>
        </ListHeader>}

        {loading ? <Result role="status">소모임 목록을 불러오는 중입니다.</Result> : result.error ? (
          <div role="alert">
            <Result>{result.error}</Result>
            <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
          </div>
        ) : (
          <>
            <SmallGroupList
              groups={result.items}
              currentPage={currentPage}
              totalPages={result.totalPages}
              onPageChange={(nextPage) => updateQuery({}, nextPage)}
              onView={handleView}
            />
            {/* AI작성: 정상 빈 결과에만 초기화를 제공하고 범위 초과 페이지 복귀는 유지한다. */}
            {result.items.length === 0 && result.totalPages === 0 && (
              <Button type="button" $variant="secondary" onClick={handleResetSearch}>검색 조건 초기화</Button>
            )}
          </>
        )}
      </Content>
    </Page>
  );
}

export default SmallGroupsPage;
