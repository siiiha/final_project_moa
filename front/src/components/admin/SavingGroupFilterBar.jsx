import styled from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
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
  min-width: 210px;
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

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
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

function SavingGroupFilterBar({ status, region, query, onStatusChange, onRegionChange, onQueryChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      <Control aria-label="계모임 상태" value={status} onChange={(event) => onStatusChange(event.target.value)}>
        <option value="전체">전체 상태</option>
        <option value="활동중">활동중</option>
        <option value="정지">정지</option>
      </Control>
      <Control aria-label="계모임 지역" value={region} onChange={(event) => onRegionChange(event.target.value)}>
        <option value="전체">전체 지역</option>
        <option value="서울">서울</option>
        <option value="경기">경기</option>
      </Control>
      <SearchInput
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        placeholder="계모임명을 입력하세요"
        aria-label="계모임 검색어"
      />
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default SavingGroupFilterBar;
