import styled from 'styled-components';
import Button from '../common/Button';

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

function AdminReportFilterBar({ query, reason, status, onQueryChange, onReasonChange, onStatusChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      <Field>
        검색어
        <Control
          type="search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="신고 대상 또는 신고자를 입력하세요"
          aria-label="회원 신고 검색어"
        />
      </Field>
      <Field>
        신고 사유
        <Select value={reason} onChange={(event) => onReasonChange(event.target.value)} aria-label="신고 사유">
          <option value="전체">전체 사유</option>
          <option value="욕설·비방">욕설·비방</option>
          <option value="광고·홍보">광고·홍보</option>
          <option value="부적절한 콘텐츠">부적절한 콘텐츠</option>
          <option value="개인정보 노출">개인정보 노출</option>
          <option value="기타">기타</option>
        </Select>
      </Field>
      <Field>
        처리 상태
        <Select value={status} onChange={(event) => onStatusChange(event.target.value)} aria-label="신고 처리 상태">
          <option value="전체">전체</option>
          <option value="대기">대기</option>
          <option value="처리 완료">처리 완료</option>
        </Select>
      </Field>
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default AdminReportFilterBar;
