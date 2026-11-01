import styled from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
`;

const Control = styled.select`
  height: 36px;
  padding: 0 9px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
`;

const SearchInput = styled.input`
  min-width: 190px;
  height: 36px;
  margin-left: auto;
  padding: 0 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus,
  & + button:focus {
    outline: 0;
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-width: 0;
    grid-column: 1 / -1;
    margin-left: 0;
  }
`;

const SearchButton = styled(Button)`
  min-height: 36px;
  padding: 8px 13px;
  font-size: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-column: 1 / -1;
  }
`;

function AdminFilterBar({
  status,
  province,
  district,
  town,
  regions,
  restrictionScope,
  query,
  onStatusChange,
  onProvinceChange,
  onDistrictChange,
  onTownChange,
  onRestrictionScopeChange,
  onQueryChange,
  onSearch,
}) {
  const provinces = [...new Set(regions.map((region) => region.provinceName))];
  const districts = regions.filter((region) => region.provinceName === province && !region.townName);
  const towns = regions.filter((region) => region.provinceName === province
    && region.districtName === district && region.townName);

  return (
    <Form onSubmit={onSearch}>
      <Control aria-label="회원 상태" value={status} onChange={(event) => onStatusChange(event.target.value)}>
        <option value="전체">전체 상태</option>
        <option value="정상">정상</option>
        <option value="제재">제재</option>
        <option value="탈퇴">탈퇴</option>
      </Control>
      <Control aria-label="가입 시·도" value={province} onChange={(event) => onProvinceChange(event.target.value)}>
        <option value="">시·도 전체</option>
        {provinces.map((item) => <option key={item} value={item}>{item}</option>)}
      </Control>
      <Control aria-label="가입 시·군·구" value={district} onChange={(event) => onDistrictChange(event.target.value)} disabled={!province}>
        <option value="">시·군·구 전체</option>
        {districts.map((item) => <option key={item.regionId} value={item.districtName}>{item.districtName || '시·군·구 전체'}</option>)}
      </Control>
      <Control aria-label="가입 읍·면·동" value={town} onChange={(event) => onTownChange(event.target.value)} disabled={!district}>
        <option value="">읍·면·동 전체</option>
        {towns.map((item) => <option key={item.regionId} value={item.townName}>{item.townName}</option>)}
      </Control>
      {/* AI작성: 현재 적용 중인 회원 기능 제한 영역으로 목록을 필터링한다. */}
      <Control
        aria-label="이용 제한 영역"
        value={restrictionScope}
        onChange={(event) => onRestrictionScopeChange(event.target.value)}
      >
        <option value="">전체 제한 영역</option>
        <option value="CONTENT">게시글·댓글</option>
        <option value="CHAT">채팅</option>
        <option value="REPORT">신고</option>
      </Control>
      <SearchInput
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        placeholder="이름 또는 이메일 검색"
        aria-label="회원 검색어"
      />
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default AdminFilterBar;
