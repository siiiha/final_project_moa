import styled from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 16px;
  display: grid;
  grid-template-columns: 156px minmax(0, 1fr) auto;
  align-items: end;
  gap: 12px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

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

function SavingGroupFilterBar({ region, keyword, regions, onRegionChange, onKeywordChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      <Field>
        활동 지역
        <Select value={region} onChange={(event) => onRegionChange(event.target.value)} aria-label="계모임 활동 지역">
          {regions.map((item) => (
            <option key={item} value={item}>
              {item === '전체' ? '전체 지역' : item}
            </option>
          ))}
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
