import styled from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 16px;
  display: grid;
  /* AI작성: 검색어·세 단계 지역·관심사를 화면 너비에 맞춰 배치한다. */
  grid-template-columns: minmax(160px, 1.5fr) repeat(4, minmax(0, 1fr)) auto;
  align-items: end;
  gap: 12px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
    padding: 14px;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
`;

const Control = styled.input`
  width: 100%;
  height: 40px;
  padding: 0 12px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Select = styled.select`
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const SearchButton = styled(Button)`
  min-height: 40px;
  padding: 8px 17px;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-column: 1 / -1;
  }
`;

// AI작성: 기존 회원 지역 조회 응답으로 선택지를 만들고 구 ID로 세종의 빈 구 이름도 구분한다.
function SmallGroupFilterBar({ keyword, province, districtId, townId, interest, regions, interests,
  regionsLoading, regionsError, onKeywordChange, onProvinceChange, onDistrictChange, onTownChange,
  onInterestChange, onSearch }) {
  const provinces = [...new Set(regions.map((item) => item.provinceName).filter(Boolean))];
  const districts = regions.filter((item) => item.provinceName === province && !item.townName);
  const district = districts.find((item) => item.regionId === Number(districtId));
  const towns = district ? regions.filter((item) => item.townName
    && item.provinceName === district.provinceName && item.districtName === district.districtName) : [];
  const unavailable = regionsLoading || Boolean(regionsError);
  return (
    <Form onSubmit={onSearch}>
      <Field>
        모임명
        <Control
          type="search"
          value={keyword}
          onChange={(event) => onKeywordChange(event.target.value)}
          placeholder="모임명을 검색하세요"
          aria-label="소모임 검색어"
        />
      </Field>
      <Field>
        시·도
        <Select value={province} onChange={(event) => onProvinceChange(event.target.value)}
          disabled={unavailable} aria-label="소모임 시·도">
          <option value="">시·도 전체</option>
          {provinces.map((item) => <option key={item} value={item}>{item}</option>)}
        </Select>
      </Field>
      <Field>
        시·군·구
        <Select value={districtId} onChange={(event) => onDistrictChange(event.target.value)}
          disabled={!province || unavailable} aria-label="소모임 시·군·구">
          <option value="">시·군·구 전체</option>
          {districts.map((item) => <option key={item.regionId} value={item.regionId}>
            {item.districtName || `${item.provinceName} 전체`}
          </option>)}
        </Select>
      </Field>
      <Field>
        읍·면·동
        <Select value={townId} onChange={(event) => onTownChange(event.target.value)}
          disabled={!districtId || unavailable} aria-label="소모임 읍·면·동">
          <option value="">읍·면·동 전체</option>
          {towns.map((item) => <option key={item.regionId} value={item.regionId}>{item.townName}</option>)}
        </Select>
      </Field>
      <Field>
        관심사
        <Select value={interest} onChange={(event) => onInterestChange(event.target.value)} aria-label="소모임 관심사">
          {interests.map((item) => <option key={item} value={item}>{item}</option>)}
        </Select>
      </Field>
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default SmallGroupFilterBar;
