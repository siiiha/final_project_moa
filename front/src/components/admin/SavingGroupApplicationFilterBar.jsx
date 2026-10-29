import styled from 'styled-components';
import { savingGroupReviewStatus, savingGroupReviewType } from '../../api/savingGroups';
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

function SavingGroupApplicationFilterBar({ type, status, query, onTypeChange, onStatusChange, onQueryChange, onSearch }) {
  return (
    <Form onSubmit={onSearch}>
      {/* AI작성: 화면 문구와 무관하게 option 값은 백엔드가 검증하는 심사 유형 코드를 그대로 사용한다. */}
      <Control aria-label="심사 유형" value={type} onChange={(event) => onTypeChange(event.target.value)}>
        <option value="">전체 유형</option>
        <option value={savingGroupReviewType.CREATE}>개설 심사</option>
        <option value={savingGroupReviewType.DELETE}>삭제 심사</option>
      </Control>
      {/* AI작성: 심사 상태도 API 코드로 유지해 페이지에서 한글값을 다시 변환하는 중복을 없앤다. */}
      <Control aria-label="심사 상태" value={status} onChange={(event) => onStatusChange(event.target.value)}>
        <option value="">전체 상태</option>
        <option value={savingGroupReviewStatus.PENDING}>대기</option>
        <option value={savingGroupReviewStatus.APPROVED}>승인</option>
        <option value={savingGroupReviewStatus.REJECTED}>반려</option>
      </Control>
      <SearchInput
        value={query}
        onChange={(event) => onQueryChange(event.target.value)}
        placeholder="계모임명을 입력하세요"
        aria-label="계모임 신청 검색어"
      />
      <SearchButton type="submit">검색</SearchButton>
    </Form>
  );
}

export default SavingGroupApplicationFilterBar;
