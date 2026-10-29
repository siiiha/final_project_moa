import styled from 'styled-components';
import Button from '../common/Button';
import SmallGroupCard from './SmallGroupCard';
import { getSmallGroupPages } from '../../api/smallGroups';

const Grid = styled.section`
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Empty = styled.p`
  margin-top: 12px;
  padding: 48px 16px;
  color: ${({ theme }) => theme.colors.muted};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 13px;
  text-align: center;
`;

const Pagination = styled.nav`
  margin-top: 22px;
  display: flex;
  justify-content: center;
  gap: 5px;
`;

const PageButton = styled(Button)`
  min-width: 30px;
`;

function SmallGroupList({ groups, currentPage, totalPages, onPageChange, onView }) {
  if (!groups.length) {
    // AI작성: 서버 응답의 마지막 페이지를 넘긴 경우와 일반적인 빈 검색 결과를 구분한다.
    if (totalPages > 0 && currentPage > totalPages) {
      return <Empty>해당 페이지에 표시할 소모임이 없습니다.{' '}
        <PageButton type="button" $variant="secondary" $size="small" onClick={() => onPageChange(1)}>첫 페이지로 이동</PageButton>
      </Empty>;
    }
    return <Empty>검색 조건에 맞는 소모임이 없습니다.</Empty>;
  }

  return (
    <>
      <Grid aria-label="소모임 목록">
        {groups.map((group) => <SmallGroupCard key={group.id} group={group} onView={onView} />)}
      </Grid>
      {totalPages > 1 && (
        <Pagination aria-label="소모임 목록 페이지 이동">
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => onPageChange(currentPage - 1)}>
            이전
          </PageButton>
          {/* AI작성: 기존 계모임과 동일하게 현재 페이지 주변의 최대 다섯 개 번호만 표시한다. */}
          {getSmallGroupPages(currentPage, totalPages).map((page) => (
            <PageButton
              key={page}
              type="button"
              $variant={page === currentPage ? 'primary' : 'secondary'}
              $size="small"
              aria-current={page === currentPage ? 'page' : undefined}
              onClick={() => onPageChange(page)}
            >
              {page}
            </PageButton>
          ))}
          <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => onPageChange(currentPage + 1)}>
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default SmallGroupList;
