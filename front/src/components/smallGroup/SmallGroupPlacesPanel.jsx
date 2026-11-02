import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { searchSmallGroupPlaces } from '../../api/smallGroups';
import Button from '../common/Button';

// AI작성: 기존 테마를 재사용하고 큰 글씨·입력 높이·줄바꿈으로 장소 정보를 읽기 쉽게 표시한다.
const Panel = styled.section`
  margin-top: 16px;
  padding: 24px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 16px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  h2 { margin: 0 0 8px; font-size: 22px; }
  h3 { margin: 0; font-size: 18px; }
  p { margin: 8px 0; }
  input, select { width: 100%; min-height: 48px; padding: 10px 12px; font: inherit;
    color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: ${({ theme }) => theme.radius.sm}; }
  input:focus-visible, select:focus-visible, a:focus-visible {
    outline: 3px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px;
  }
  input:disabled, select:disabled { opacity: 0.65; cursor: not-allowed; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { padding: 20px 16px; }
`;
const SearchForm = styled.form`
  display: grid;
  gap: 16px;
  margin-top: 20px;
  label { display: grid; gap: 6px; font-weight: 700; min-width: 0; }
`;
const Row = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  label { flex: 1 1 180px; }
`;
const Action = styled(Button)`
  min-height: 48px;
  font-size: 16px;
  &:disabled { opacity: 0.6; cursor: not-allowed; }
`;
const Hint = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
`;
const Alert = styled.p`
  color: ${({ theme }) => theme.colors.dangerDark};
  font-weight: 700;
`;
const Results = styled.ul`
  list-style: none;
  padding: 0;
  margin: 16px 0;
  li { padding: 20px 0; border-bottom: 1px solid ${({ theme }) => theme.colors.border}; }
  a { display: inline-flex; align-items: center; min-height: 48px;
    color: ${({ theme }) => theme.colors.primaryDark}; text-decoration: underline; font-weight: 700; }
`;
const initialResult = { data: null, error: '', code: '', loading: false };

// AI작성: 부모의 소모임·역할·인증 버전 key가 바뀌면 위치와 검색 상태를 모두 폐기한다.
function SmallGroupPlacesPanel({ smallGroupId }) {
  const [query, setQuery] = useState('');
  const [position, setPosition] = useState(null);
  const [radius, setRadius] = useState('');
  const [sort, setSort] = useState('accuracy');
  const [locating, setLocating] = useState(false);
  const [locationError, setLocationError] = useState('');
  const [result, setResult] = useState(initialResult);
  const [lastSearch, setLastSearch] = useState(null);
  const requestRef = useRef(null);
  const locationVersion = useRef(0);
  const forbidden = ['SMALL_GROUP_PLACE_FORBIDDEN', 'LOGIN_REQUIRED', 'INVALID_TOKEN', 'ACCESS_DENIED'].includes(result.code);

  useEffect(() => () => {
    requestRef.current?.abort();
    locationVersion.current += 1;
  }, []);

  function resetSearch() {
    requestRef.current?.abort();
    setLastSearch(null);
    setResult(initialResult);
  }

  function clearLocation() {
    locationVersion.current += 1;
    setPosition(null);
    setRadius('');
    setSort('accuracy');
    setLocating(false);
    setLocationError('');
    resetSearch();
  }

  // AI작성: 자동 위치 수집 없이 명시적 버튼 클릭 때만 기존 회원 인증과 같은 브라우저 위치 옵션을 사용한다.
  function findLocation() {
    clearLocation();
    if (!navigator.geolocation) {
      setLocationError('현재 브라우저에서는 위치 확인을 지원하지 않습니다. 검색어만으로 검색해 주세요.');
      return;
    }
    const version = ++locationVersion.current;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(({ coords }) => {
      if (version !== locationVersion.current) return;
      setLocating(false);
      if (!Number.isFinite(coords.latitude) || Math.abs(coords.latitude) > 90
          || !Number.isFinite(coords.longitude) || Math.abs(coords.longitude) > 180
          || !Number.isFinite(coords.accuracy) || coords.accuracy <= 0 || coords.accuracy > 500) {
        setLocationError('위치가 정확하지 않습니다. 예상 오차가 500m 이하일 때 다시 확인하거나 검색어만으로 검색해 주세요.');
        return;
      }
      setPosition({ latitude: coords.latitude, longitude: coords.longitude });
    }, (error) => {
      if (version !== locationVersion.current) return;
      setLocating(false);
      const messages = {
        1: '위치 사용이 허용되지 않았습니다. 검색어만으로도 검색할 수 있습니다.',
        2: '현재 위치를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.',
        3: '위치 확인 시간이 초과됐습니다. 다시 시도해 주세요.',
      };
      setLocationError(messages[error.code] || '위치를 확인하지 못했습니다. 검색어만으로 검색해 주세요.');
    }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
  }

  async function search(criteria, page = 1) {
    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setLastSearch({ criteria, page });
    setResult({ ...initialResult, loading: true });
    try {
      const data = await searchSmallGroupPlaces(smallGroupId, { ...criteria, page, size: 15, signal: controller.signal });
      if (!controller.signal.aborted) setResult({ ...initialResult, data });
    } catch (error) {
      if (controller.signal.aborted || error.code === 'ERR_CANCELED') return;
      setResult({ ...initialResult, error: error.message, code: error.code });
      // AI작성: 접근 불가가 확인되면 위치와 재시도 조건에서도 사용자 좌표를 즉시 제거한다.
      if (['SMALL_GROUP_PLACE_FORBIDDEN', 'LOGIN_REQUIRED', 'INVALID_TOKEN', 'ACCESS_DENIED'].includes(error.code)) {
        setPosition(null);
        setRadius('');
        setSort('accuracy');
        setLastSearch(null);
      }
    }
  }

  function submit(event) {
    event.preventDefault();
    if (forbidden || locating) return;
    const trimmed = query.trim();
    if (!trimmed || trimmed.length > 100) {
      resetSearch();
      setResult({ ...initialResult, error: '장소 검색어를 1~100자로 입력해 주세요.', code: 'BAD_REQUEST' });
      return;
    }
    search({ query: trimmed, sort, ...(position || {}), ...(position && radius !== '' ? { radius: Number(radius) } : {}) });
  }

  return (
    <Panel aria-label="소모임 장소 찾기">
      <h2>장소 찾기</h2>
      <Hint>지역명과 장소명을 함께 입력하면 원하는 장소를 찾기 쉽습니다.</Hint>
      <SearchForm onSubmit={submit} noValidate>
        <label>장소 검색어
          <input type="search" value={query} maxLength={100} required placeholder="지역명, 카페, 공원 등"
            onChange={(event) => setQuery(event.target.value)} disabled={forbidden} />
        </label>
        <div>
          <Hint id="place-location-purpose">내 위치를 사용해 검색하면 주변 장소 조회를 위해 위치를 카카오에 전송합니다.
            위치는 저장하지 않으며, 검색어만으로도 검색할 수 있습니다.</Hint>
          <Row>
            <Action type="button" $variant="secondary" onClick={findLocation} disabled={locating || forbidden}
              aria-describedby="place-location-purpose">{locating ? '위치 확인 중…' : position ? '내 위치 다시 확인' : '내 위치 사용'}</Action>
            {(position || locating) && <Action type="button" $variant="secondary" onClick={clearLocation}>내 위치 사용 해제</Action>}
          </Row>
          {position && <Hint role="status">현재 위치를 확인했습니다. 검색 버튼을 누르면 위치를 사용합니다.</Hint>}
          {locationError && <Alert role="alert">{locationError}</Alert>}
        </div>
        <Row>
          <label>검색 반경
            <select value={radius} onChange={(event) => setRadius(event.target.value)} disabled={!position || forbidden}>
              <option value="">제한 없음</option>
              {[1, 3, 5, 10, 20].map((km) => <option key={km} value={km * 1000}>{km}km 이내</option>)}
            </select>
          </label>
          <label>정렬 기준
            <select value={sort} onChange={(event) => setSort(event.target.value)} disabled={forbidden}>
              <option value="accuracy">관련도순</option>
              <option value="distance" disabled={!position}>거리순</option>
            </select>
          </label>
        </Row>
        {!position && <Hint>내 위치를 사용하면 검색 반경과 거리순 정렬을 선택할 수 있습니다.</Hint>}
        <Row><Action type="submit" disabled={locating || forbidden}>{result.loading ? '다시 검색' : '장소 검색'}</Action></Row>
      </SearchForm>
      <div aria-busy={result.loading} aria-label="장소 검색 결과">
        {result.loading ? <Hint role="status">장소를 검색하는 중입니다.</Hint>
          : result.error ? <div role="alert"><Alert>{result.error}</Alert>
            {forbidden ? <Hint>소모임 상세 화면에서 로그인 상태와 가입 상태를 다시 확인해 주세요.</Hint>
              : lastSearch && <Action type="button" $variant="secondary" onClick={() => search(lastSearch.criteria, lastSearch.page)}>다시 시도</Action>}
          </div>
            : result.data ? <>
              <Hint role="status">‘{lastSearch.criteria.query}’ 검색 결과 · {result.data.page}페이지</Hint>
              <Results>{result.data.items.map((place) => <li key={place.placeId}>
                <h3>{place.placeName}</h3>
                {place.category && <Hint>{place.category}</Hint>}
                <p>{place.roadAddress || place.address || '주소 정보가 없습니다.'}</p>
                {place.roadAddress && place.address && place.roadAddress !== place.address && <Hint>지번: {place.address}</Hint>}
                {place.distanceMeters != null && <p>현재 위치 기준 {place.distanceMeters.toLocaleString()}m</p>}
                <a href={place.placeUrl} target="_blank" rel="noopener noreferrer" referrerPolicy="no-referrer">장소 상세 보기 (새 창)</a>
              </li>)}</Results>
              <Row aria-label="검색 결과 페이지 이동">
                <Action type="button" $variant="secondary" disabled={result.data.page <= 1}
                  onClick={() => search(lastSearch.criteria, result.data.page - 1)}>이전 페이지</Action>
                <span>{result.data.page}페이지</span>
                <Action type="button" $variant="secondary" disabled={result.data.end || result.data.page >= 45}
                  onClick={() => search(lastSearch.criteria, result.data.page + 1)}>다음 페이지</Action>
              </Row>
            </> : <Hint role="status">검색어를 입력하고 장소 검색을 눌러 주세요.</Hint>}
      </div>
      {/* AI작성: TODO 길찾기는 후속 요청에서 구현하며 검색 결과를 모임 장소로 저장하지 않는다. */}
      <Hint>길찾기는 준비 중입니다.</Hint>
    </Panel>
  );
}

export default SmallGroupPlacesPanel;
