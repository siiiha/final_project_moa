import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { FiChevronLeft, FiChevronRight, FiMap, FiMapPin, FiNavigation, FiSearch } from 'react-icons/fi';
import { searchSmallGroupPlaces } from '../../api/smallGroups';
import Button from '../common/Button';
import RegionMap from '../member/RegionMap';

// AI작성: 모아 테마를 유지하면서 검색 도구·장소 목록·선택 장소 지도를 분리한 탐색 화면으로 구성한다.
const Panel = styled.section`
  margin-top: 16px;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 16px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  h2 { margin: 0; font-size: 22px; letter-spacing: -0.6px; }
  h3 { margin: 0; font-size: 18px; }
  p { margin: 8px 0; }
  input, select { width: 100%; min-height: 48px; min-width: 0; padding: 10px 12px; font: inherit;
    color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: ${({ theme }) => theme.radius.sm}; }
  input:focus-visible, select:focus-visible, a:focus-visible {
    outline: 3px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px;
  }
  input:disabled, select:disabled { opacity: 0.65; cursor: not-allowed; }
`;
const Header = styled.header`
  padding: 24px 24px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  > svg { flex-shrink: 0; width: 44px; height: 44px; padding: 10px;
    color: ${({ theme }) => theme.colors.primary}; background: ${({ theme }) => theme.colors.primarySoft};
    border-radius: ${({ theme }) => theme.radius.md}; }
  p { margin: 3px 0 0; color: ${({ theme }) => theme.colors.secondaryText}; font-size: 14px; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { padding: 20px 16px 0; }
`;
const SearchForm = styled.form`
  display: grid;
  gap: 14px;
  padding: 20px 24px;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  label { display: grid; gap: 6px; font-weight: 700; min-width: 0; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { padding: 20px 16px; }
`;
const SearchBar = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 6px 6px 16px;
  border: 2px solid ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  > svg { flex-shrink: 0; color: ${({ theme }) => theme.colors.primary}; }
  > label { flex: 1; }
  label > span { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
  input { border: 0; padding: 8px 0; background: transparent; font-weight: 500; }
  &:focus-within { box-shadow: 0 0 0 3px ${({ theme }) => theme.colors.primarySoft}; }
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) { gap: 8px; padding-left: 10px; > svg { display: none; } }
`;
const Row = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  label { flex: 1 1 150px; }
`;
const Action = styled(Button)`
  min-height: 48px;
  font-size: 16px;
  &:disabled { opacity: 0.6; cursor: not-allowed; }
`;
const Filters = styled.div`
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 12px;
  label { flex: 1 1 140px; font-size: 14px; }
  > div { display: flex; flex-wrap: wrap; gap: 8px; }
`;
const Hint = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
`;
const LocationHint = styled(Hint)`
  font-size: 14px;
  margin: 0 !important;
`;
const Alert = styled.p`
  color: ${({ theme }) => theme.colors.dangerDark};
  font-weight: 700;
`;
// AI작성: 데스크톱은 목록·지도 두 열, 모바일은 지도 다음 목록 순서로 읽히도록 배치한다.
const Workspace = styled.div`
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(0, 1.4fr);
  grid-template-areas: 'results map';
  align-items: start;
  background: ${({ theme }) => theme.colors.background};
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas: 'map' 'results';
  }
`;
const ResultPane = styled.div`
  grid-area: results;
  min-width: 0;
  background: ${({ theme }) => theme.colors.white};
  border-right: 1px solid ${({ theme }) => theme.colors.border};
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { border-right: 0; border-top: 1px solid ${({ theme }) => theme.colors.border}; }
`;
const ResultHeading = styled.div`
  padding: 18px 20px;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  p { margin: 0; font-weight: 700; }
  span { color: ${({ theme }) => theme.colors.secondaryText}; font-size: 14px; font-weight: 400; }
`;
const Results = styled.ol`
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 600px;
  overflow-y: auto;
  li { border-bottom: 1px solid ${({ theme }) => theme.colors.border}; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { max-height: none; }
`;
const PlaceButton = styled.button`
  display: flex;
  align-items: start;
  gap: 12px;
  width: 100%;
  padding: 20px 16px;
  border: 0;
  border-left: 3px solid ${({ $selected, theme }) => $selected ? theme.colors.primary : 'transparent'};
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $selected, theme }) => $selected ? theme.colors.primarySoft : theme.colors.white};
  text-align: left;
  font: inherit;
  cursor: pointer;
  &:hover { background: ${({ theme }) => theme.colors.primarySoft}; }
  > span:last-child { display: grid; gap: 7px; min-width: 0; flex: 1; }
  strong { font-size: 18px; line-height: 1.4; }
  small { color: ${({ theme }) => theme.colors.secondaryText}; font-size: 14px; }
`;
const PlaceNumber = styled.span`
  display: inline-grid;
  place-items: center;
  flex-shrink: 0;
  width: 30px;
  height: 34px;
  border-radius: 10px 10px 10px 2px;
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.white};
  font-weight: 700;
`;
const PlaceStatus = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: ${({ theme }) => theme.colors.primaryDark};
  font-size: 14px;
  font-weight: 700;
`;
const EmptyState = styled.div`
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 48px 24px;
  text-align: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  > svg { width: 44px; height: 44px; margin-bottom: 8px; color: ${({ theme }) => theme.colors.primary}; }
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 18px; }
  p { margin: 0; }
`;
const Pagination = styled(Row)`
  justify-content: center;
  padding: 16px 12px;
  gap: 8px;
  button { padding: 10px 12px; }
  span { min-width: 24px; text-align: center; }
`;
const MapPane = styled.aside`
  grid-area: map;
  min-width: 0;
  position: sticky;
  top: 16px;
  padding: 20px;
  > header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-weight: 700; }
  > header svg { color: ${({ theme }) => theme.colors.primary}; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { position: static; padding: 16px; }
`;
// AI작성: 기존 지역 지도 컴포넌트를 수정하지 않고 선택 장소 좌표만 전달해 재사용한다.
const MapViewport = styled.div`
  [aria-label='활동 지역 지도'] { height: 420px; }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { [aria-label='활동 지역 지도'] { height: 260px; } }
`;
const MapPlaceholder = styled(EmptyState)`
  min-height: 420px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  align-content: center;
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) { min-height: 220px; }
`;
const SelectedPlace = styled.div`
  margin-top: 14px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  h3 { display: flex; align-items: start; gap: 10px; }
`;
const Footer = styled(Hint)`
  margin: 0 !important;
  padding: 14px 20px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
  background: ${({ theme }) => theme.colors.background};
  font-size: 14px;
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
  // AI작성: 선택 상태는 화면에만 보관하며 첫 검색 결과를 기본 선택한다. 모임 장소를 저장하지 않는다.
  const [selectedPlaceId, setSelectedPlaceId] = useState(null);
  const selectedPlace = result.data?.items.find((place) => place.placeId === selectedPlaceId) || result.data?.items[0];
  const selectedIndex = selectedPlace ? result.data.items.indexOf(selectedPlace) + 1 : 0;
  const requestRef = useRef(null);
  const locationVersion = useRef(0);
  // AI작성: 현재 위치는 길찾기 콜백에서만 사용하며 상태·저장소에는 보관하지 않는다.
  const directionsRef = useRef(null);
  const [directionsState, setDirectionsState] = useState({ loading: false, error: '' });
  const forbidden = ['SMALL_GROUP_PLACE_FORBIDDEN', 'LOGIN_REQUIRED', 'INVALID_TOKEN', 'ACCESS_DENIED'].includes(result.code);

  useEffect(() => () => {
    requestRef.current?.abort();
    locationVersion.current += 1;
    directionsRef.current?.popup.close();
    directionsRef.current = null;
  }, []);

  // AI작성: 검색·장소 변경 시 대기 중인 새 창을 닫고 뒤늦은 위치 응답의 외부 이동을 막는다.
  function cancelDirections() {
    directionsRef.current?.popup.close();
    directionsRef.current = null;
    setDirectionsState({ loading: false, error: '' });
  }

  function resetSearch() {
    cancelDirections();
    requestRef.current?.abort();
    setLastSearch(null);
    setResult(initialResult);
    setSelectedPlaceId(null);
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
    cancelDirections();
    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setLastSearch({ criteria, page });
    setSelectedPlaceId(null);
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

  // AI작성: 사용자 클릭 시 빈 창을 먼저 확보해 비동기 위치 확인 후의 팝업 차단을 피한다.
  function openDirections() {
    if (!selectedPlace || forbidden || directionsRef.current) return;
    setDirectionsState({ loading: false, error: '' });
    if (!navigator.geolocation) {
      setDirectionsState({ loading: false, error: '현재 브라우저에서는 위치 확인을 지원하지 않습니다.' });
      return;
    }
    let popup;
    try {
      popup = window.open('about:blank', '_blank');
      if (!popup) {
        setDirectionsState({ loading: false, error: '새 창이 차단됐습니다. 이 사이트의 팝업을 허용한 뒤 다시 눌러 주세요.' });
        return;
      }
      // AI작성: 새 창에서 모아 창에 접근하지 못하도록 분리하고 외부 이동 시 referrer를 보내지 않는다.
      popup.opener = null;
      const referrer = popup.document.createElement('meta');
      referrer.name = 'referrer';
      referrer.content = 'no-referrer';
      popup.document.head.appendChild(referrer);
      popup.document.title = '길찾기 준비 중';
      popup.document.body.textContent = '현재 위치를 확인하고 있습니다. 잠시만 기다려 주세요.';
    } catch {
      popup?.close();
      setDirectionsState({ loading: false, error: '길찾기 창을 열지 못했습니다. 다시 눌러 주세요.' });
      return;
    }
    const pending = { popup };
    const destination = selectedPlace;
    directionsRef.current = pending;
    setDirectionsState({ loading: true, error: '' });
    function fail(message) {
      if (directionsRef.current !== pending) return;
      directionsRef.current = null;
      popup.close();
      setDirectionsState({ loading: false, error: message });
    }
    try {
      navigator.geolocation.getCurrentPosition(({ coords }) => {
        if (directionsRef.current !== pending) return;
        if (popup.closed) {
          fail('길찾기 창이 닫혔습니다. 다시 눌러 주세요.');
          return;
        }
        if (!Number.isFinite(coords.latitude) || Math.abs(coords.latitude) > 90
            || !Number.isFinite(coords.longitude) || Math.abs(coords.longitude) > 180
            || !Number.isFinite(coords.accuracy) || coords.accuracy <= 0 || coords.accuracy > 500) {
          fail('위치가 정확하지 않습니다. 예상 오차가 500m 이하일 때 다시 눌러 주세요.');
          return;
        }
        try {
          // AI작성: 공식 길찾기 URL은 위도·경도 순서이며 장소명은 경로 변수로 인코딩한다.
          const url = `https://map.kakao.com/link/from/${encodeURIComponent('현재 위치')},${coords.latitude},${coords.longitude}`
            + `/to/${encodeURIComponent(destination.placeName)},${destination.latitude},${destination.longitude}`;
          popup.location.replace(url);
          directionsRef.current = null;
          setDirectionsState({ loading: false, error: '' });
        } catch {
          fail('카카오맵 길찾기를 열지 못했습니다. 다시 눌러 주세요.');
        }
      }, (error) => {
        const messages = {
          1: '위치 사용이 허용되지 않았습니다. 위치 권한을 허용한 뒤 다시 눌러 주세요.',
          2: '현재 위치를 확인할 수 없습니다. 잠시 후 다시 눌러 주세요.',
          3: '위치 확인 시간이 초과됐습니다. 다시 눌러 주세요.',
        };
        fail(messages[error.code] || '현재 위치를 확인하지 못했습니다. 다시 눌러 주세요.');
      }, { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
    } catch {
      fail('현재 위치를 확인하지 못했습니다. 다시 눌러 주세요.');
    }
  }

  return (
    <Panel aria-label="소모임 장소 찾기">
      <Header>
        <FiMapPin aria-hidden="true" />
        <div><h2>우리 모임, 어디서 만날까요?</h2><p>장소를 검색하고 지도에서 위치를 확인해 보세요.</p></div>
      </Header>
      <SearchForm onSubmit={submit} noValidate>
        <SearchBar>
          <FiSearch size={22} aria-hidden="true" />
          <label><span>장소 검색어</span>
            <input type="search" value={query} maxLength={100} required placeholder="지역명 또는 장소명 검색"
              onChange={(event) => setQuery(event.target.value)} disabled={forbidden} />
          </label>
          <Action type="submit" disabled={locating || forbidden}>{result.loading ? '다시 검색' : '장소 검색'}</Action>
        </SearchBar>
        <Filters>
          <div>
            <Action type="button" $variant="secondary" onClick={findLocation} disabled={locating || forbidden}
              aria-describedby="place-location-purpose"><FiNavigation aria-hidden="true" />{locating ? '위치 확인 중…' : position ? '내 위치 다시 확인' : '내 위치 사용'}</Action>
            {(position || locating) && <Action type="button" $variant="secondary" onClick={clearLocation}>내 위치 사용 해제</Action>}
          </div>
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
        </Filters>
        <LocationHint id="place-location-purpose">내 위치를 사용해 검색하면 위치를 카카오에 전송하며 저장하지 않습니다.
          {!position && ' 위치 사용 없이 검색어만으로도 검색할 수 있습니다.'}</LocationHint>
        {position && <LocationHint role="status">현재 위치를 확인했습니다. 검색 버튼을 누르면 위치를 사용합니다.</LocationHint>}
        {locationError && <Alert role="alert">{locationError}</Alert>}
      </SearchForm>
      <Workspace>
        <ResultPane aria-busy={result.loading} aria-label="장소 검색 결과">
          <ResultHeading><p>{result.data ? `‘${lastSearch.criteria.query}’ 검색 결과` : '검색 결과'}</p>
            <span>{result.data ? `${result.data.items.length}곳 · ${result.data.page}페이지 · 장소를 누르면 지도에 표시됩니다.` : '모임에 어울리는 장소를 찾아보세요.'}</span>
          </ResultHeading>
          {result.loading ? <EmptyState role="status"><FiSearch aria-hidden="true" /><strong>장소를 검색하는 중입니다.</strong><p>잠시만 기다려 주세요.</p></EmptyState>
            : result.error ? <EmptyState role="alert"><FiMapPin aria-hidden="true" /><Alert>{result.error}</Alert>
              {forbidden ? <p>소모임 상세 화면에서 로그인 상태와 가입 상태를 다시 확인해 주세요.</p>
                : lastSearch && <Action type="button" $variant="secondary" onClick={() => search(lastSearch.criteria, lastSearch.page)}>다시 시도</Action>}
            </EmptyState>
              : result.data ? <>
                <Results>{result.data.items.map((place, index) => <li key={place.placeId}>
                  <PlaceButton type="button" $selected={place.placeId === selectedPlace.placeId}
                    aria-pressed={place.placeId === selectedPlace.placeId} onClick={() => {
                      if (place.placeId !== selectedPlace.placeId) cancelDirections();
                      setSelectedPlaceId(place.placeId);
                    }}>
                    <PlaceNumber aria-hidden="true">{index + 1}</PlaceNumber>
                    <span><strong>{place.placeName}</strong>
                      {place.category && <small>{place.category}</small>}
                      <span>{place.roadAddress || place.address || '주소 정보가 없습니다.'}</span>
                      {place.distanceMeters != null && <small>현재 위치 기준 {place.distanceMeters.toLocaleString()}m</small>}
                      <PlaceStatus><FiMapPin aria-hidden="true" />{place.placeId === selectedPlace.placeId ? '지도에 표시 중' : '지도에서 보기'}</PlaceStatus>
                    </span>
                  </PlaceButton>
                </li>)}</Results>
                <Pagination aria-label="검색 결과 페이지 이동">
                  <Action type="button" $variant="secondary" aria-label="이전 페이지" disabled={result.data.page <= 1}
                    onClick={() => search(lastSearch.criteria, result.data.page - 1)}><FiChevronLeft aria-hidden="true" />이전</Action>
                  <span aria-label="현재 페이지">{result.data.page}</span>
                  <Action type="button" $variant="secondary" aria-label="다음 페이지" disabled={result.data.end || result.data.page >= 45}
                    onClick={() => search(lastSearch.criteria, result.data.page + 1)}>다음<FiChevronRight aria-hidden="true" /></Action>
                </Pagination>
              </> : <EmptyState role="status"><FiSearch aria-hidden="true" /><strong>어디에서 만나고 싶으세요?</strong><p>지역명과 카페, 공원 등<br />찾고 싶은 장소를 검색해 보세요.</p></EmptyState>}
        </ResultPane>
        <MapPane aria-label="선택 장소 지도">
          <header><FiMap aria-hidden="true" />지도에서 위치 확인</header>
          {selectedPlace ? <>
            <MapViewport><RegionMap key={`${selectedPlace.placeId}:${selectedPlace.latitude}:${selectedPlace.longitude}`}
              latitude={selectedPlace.latitude} longitude={selectedPlace.longitude} /></MapViewport>
            <SelectedPlace aria-live="polite">
              <h3><PlaceNumber aria-hidden="true">{selectedIndex}</PlaceNumber>{selectedPlace.placeName}</h3>
              {selectedPlace.category && <Hint>{selectedPlace.category}</Hint>}
              <p>{selectedPlace.roadAddress || selectedPlace.address || '주소 정보가 없습니다.'}</p>
              {selectedPlace.roadAddress && selectedPlace.address && selectedPlace.roadAddress !== selectedPlace.address && <Hint>지번: {selectedPlace.address}</Hint>}
              {selectedPlace.distanceMeters != null && <p>현재 위치 기준 {selectedPlace.distanceMeters.toLocaleString()}m</p>}
              <LocationHint id="place-directions-purpose">누르면 현재 위치를 확인해 카카오맵에 출발지로 전달합니다. 위치는 저장하지 않으며 새 창으로 열립니다.</LocationHint>
              <Action type="button" onClick={openDirections} disabled={directionsState.loading || forbidden}
                aria-busy={directionsState.loading} aria-describedby="place-directions-purpose">
                <FiNavigation aria-hidden="true" />현 위치에서 길찾기
              </Action>
              {directionsState.loading && <Hint role="status">출발지 위치를 확인하는 중입니다.</Hint>}
              {directionsState.error && <Alert role="alert">{directionsState.error}</Alert>}
            </SelectedPlace>
          </> : <MapPlaceholder><FiMap aria-hidden="true" /><strong>만날 장소를 지도에서 확인하세요</strong><p>검색한 장소를 선택하면<br />이곳에 위치를 표시해 드립니다.</p></MapPlaceholder>}
        </MapPane>
      </Workspace>
      {/* AI작성: 길찾기는 카카오맵에서 안내하고 검색 결과를 모임 장소로 저장하지 않는다. */}
      <Footer>선택한 장소까지의 길찾기는 카카오맵에서 확인할 수 있습니다.</Footer>
    </Panel>
  );
}

export default SmallGroupPlacesPanel;
