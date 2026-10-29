import styled from 'styled-components';
import Button from '../common/Button';
import { savingGroupRegionOptions } from '../../api/savingGroups';

const Form = styled.form`
  display: grid;
  grid-template-columns: minmax(220px, 1.65fr) minmax(150px, 1fr) minmax(150px, 1fr) auto;
  align-items: end;
  gap: 12px 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.laptop}) {
    grid-template-columns: 1fr 1fr;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const Control = styled.input`
  width: 100%;
  height: 42px;
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
  height: 42px;
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
  min-height: 42px;
  padding: 9px 16px;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    width: 100%;
  }
`;

function AdminCommentFilterBar({ query, region, status, onQueryChange, onRegionChange, onStatusChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      <Field>
        검색어
        <Control
          type="search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="댓글, 작성자, 게시글 제목을 입력하세요"
          aria-label="댓글 검색어"
        />
      </Field>
      <Field>
        지역
        <Select value={region} onChange={(event) => onRegionChange(event.target.value)} aria-label="댓글 작성자 지역">
          <option value="">전체 지역</option>
          {/* AI작성: 관리자 댓글 필터도 기존 서비스 지역 목록을 재사용한다. */}
          {savingGroupRegionOptions.map((item) => (
            <option key={item.code} value={item.name}>
              {item.name}
            </option>
          ))}
        </Select>
      </Field>
      <Field>
        상태
        <Select value={status} onChange={(event) => onStatusChange(event.target.value)} aria-label="댓글 상태">
          <option value="">전체</option>
          <option value="VISIBLE">공개</option>
          <option value="DELETED">삭제</option>
        </Select>
      </Field>
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default AdminCommentFilterBar;
