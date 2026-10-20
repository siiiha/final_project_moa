import styled from 'styled-components';
import SavingGroupCard from './SavingGroupCard';

const Grid = styled.section`
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Empty = styled.p`
  margin-top: 18px;
  padding: 48px 16px;
  color: ${({ theme }) => theme.colors.muted};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 13px;
  text-align: center;
`;

const Pagination = styled.nav`
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 5px;
`;

const PageButton = styled.button`
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  color: ${({ $active, theme }) => ($active ? theme.colors.white : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

function SavingGroupList({ groups, currentPage, totalPages, onPageChange }) {
  if (!groups.length) {
    return <Empty>검색 조건에 맞는 계모임이 없습니다.</Empty>;
  }

  return (
    <>
      <Grid aria-label="계모임 목록">
        {groups.map((group) => (
          <SavingGroupCard key={group.id} group={group} />
        ))}
      </Grid>
      {totalPages > 1 && (
        <Pagination aria-label="계모임 목록 페이지">
          {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
            <PageButton
              key={page}
              type="button"
              $active={page === currentPage}
              onClick={() => onPageChange(page)}
              aria-current={page === currentPage ? 'page' : undefined}
            >
              {page}
            </PageButton>
          ))}
        </Pagination>
      )}
    </>
  );
}

export default SavingGroupList;
