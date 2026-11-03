import styled from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 16px;
  display: grid;
  /* AI작성: 세 단계 지역 선택과 검색어가 좁은 화면에서 순서대로 배치되도록 한다. */
  grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(180px, 2fr) auto;
  align-items: end;
  gap: 12px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: 900px) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
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

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

// AI작성: 구 선택은 지역 ID로 구분해 구 이름이 비어 있는 세종도 동을 선택할 수 있게 한다.
function SavingGroupFilterBar({ province, districtId, townId, keyword, regions, regionsLoading,
  regionsError, onProvinceChange, onDistrictChange, onTownChange, onKeywordChange, onSearch }) {
  const provinces = [...new Set(regions.map((item) => item.provinceName).filter(Boolean))];
  const districts = regions.filter((item) => item.provinceName === province && !item.townName);
  const district = districts.find((item) => item.regionId === Number(districtId));
  const towns = district ? regions.filter((item) => item.townName
    && item.provinceName === district.provinceName && item.districtName === district.districtName) : [];
  const unavailable = regionsLoading || Boolean(regionsError);

  return (
    <Form onSubmit={onSearch}>
      <Field>
        시·도
        <Select value={province} onChange={(event) => onProvinceChange(event.target.value)}
          disabled={unavailable} aria-label="계모임 시·도">
          <option value="">시·도 전체</option>
          {provinces.map((item) => <option key={item} value={item}>{item}</option>)}
        </Select>
      </Field>
      <Field>
        시·군·구
        <Select value={districtId} onChange={(event) => onDistrictChange(event.target.value)}
          disabled={!province || unavailable} aria-label="계모임 시·군·구">
          <option value="">시·군·구 전체</option>
          {districts.map((item) => <option key={item.regionId} value={item.regionId}>
            {item.districtName || `${item.provinceName} 전체`}
          </option>)}
        </Select>
      </Field>
      <Field>
        읍·면·동
        <Select value={townId} onChange={(event) => onTownChange(event.target.value)}
          disabled={!districtId || unavailable} aria-label="계모임 읍·면·동">
          <option value="">읍·면·동 전체</option>
          {towns.map((item) => <option key={item.regionId} value={item.regionId}>{item.townName}</option>)}
        </Select>
      </Field>
      <Field>
        모임 검색
        <Control
          type="search"
          value={keyword}
          onChange={(event) => onKeywordChange(event.target.value)}
          placeholder="계모임 이름 또는 소개를 입력하세요"
          aria-label="계모임 검색어"
        />
      </Field>
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default SavingGroupFilterBar;
