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

function AdminReportFilterBar({ query, searchType, reportType, status, onQueryChange, onSearchTypeChange, onReportTypeChange, onStatusChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      <Field>
        검색어
        <Control
          type="search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="검색어를 입력하세요"
          aria-label="회원 신고 검색어"
        />
      </Field>
      <Field>
        검색 대상
        <Select value={searchType} onChange={(event) => onSearchTypeChange(event.target.value)} aria-label="신고 검색 대상">
          <option value="reportedMember">신고 대상 회원</option><option value="reporter">신고자</option><option value="processReason">처리 사유</option>
        </Select>
      </Field>
      <Field>
        신고 유형
        <Select value={reportType} onChange={(event) => onReportTypeChange(event.target.value)} aria-label="신고 유형">
          <option value="">전체 유형</option><option value="BOARD_POST">게시글</option><option value="BOARD_COMMENT">댓글</option><option value="BOARD_MEMBER">회원</option><option value="MEETING">모임</option><option value="MEETING_MEMBER">모임 회원</option>
        </Select>
      </Field>
      <Field>
        처리 상태
        <Select value={status} onChange={(event) => onStatusChange(event.target.value)} aria-label="신고 처리 상태">
          <option value="">전체</option><option value="PENDING">대기</option><option value="CONFIRMED">승인</option><option value="REJECTED">반려</option>
        </Select>
      </Field>
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default AdminReportFilterBar;
