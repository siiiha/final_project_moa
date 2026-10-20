import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  margin-top: 12px;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const TableWrap = styled.div`
  padding: 12px;
`;

const Table = styled.table`
  width: 100%;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-collapse: separate;
  border-spacing: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  table-layout: fixed;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    text-align: center;
  }

  th {
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(2) {
    color: ${({ theme }) => theme.colors.text};
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }

  th:first-child {
    border-top-left-radius: ${({ theme }) => theme.radius.sm};
  }

  th:last-child {
    border-top-right-radius: ${({ theme }) => theme.radius.sm};
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    th,
    td {
      padding: 10px 5px;
      font-size: 11px;
    }

    th:nth-child(3),
    td:nth-child(3),
    th:nth-child(5),
    td:nth-child(5) {
      display: none;
    }
  }
`;

const Category = styled.span`
  display: inline-flex;
  max-width: 100%;
  padding: 4px 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
`;

const PostTitle = styled.button`
  width: 100%;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.text};
  background: transparent;
  border: 0;
  font-size: inherit;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    text-decoration: underline;
  }
`;

const Empty = styled.p`
  padding: 48px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Pagination = styled.nav`
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 4px;
`;

const PageButton = styled(Button)`
  min-width: 30px;
`;

function PostTable({ posts, totalCount, currentPage, pageNumbers, onPageChange, onPostClick }) {
  return (
    <>
      <Card>
        {posts.length ? (
          <TableWrap>
            <Table>
              <colgroup>
                <col width="18%" />
                <col width="auto" />
                <col width="15%" />
                <col width="13%" />
                <col width="10%" />
              </colgroup>
              <thead>
                <tr>
                  <th scope="col">분류</th>
                  <th scope="col">제목</th>
                  <th scope="col">작성자</th>
                  <th scope="col">작성일</th>
                  <th scope="col">조회</th>
                </tr>
              </thead>
              <tbody>
                {posts.map((post) => (
                  <tr key={post.id}>
                    <td>
                      <Category>{post.category}</Category>
                    </td>
                    <td>
                      <PostTitle type="button" onClick={() => onPostClick(post.id)}>
                        {post.title}
                      </PostTitle>
                    </td>
                    <td>{post.author}</td>
                    <td>{post.date.slice(5)}</td>
                    <td>{post.views}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableWrap>
        ) : (
          <Empty>검색 결과가 없습니다.</Empty>
        )}
      </Card>
      {totalCount > 0 && (
        <Pagination aria-label="게시글 페이지 이동">
          <PageButton
            type="button"
            $variant="secondary"
            $size="small"
            disabled={currentPage === 1}
            onClick={() => onPageChange(currentPage - 1)}
          >
            이전
          </PageButton>
          {pageNumbers.map((page) => (
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
          <PageButton
            type="button"
            $variant="secondary"
            $size="small"
            disabled={currentPage === pageNumbers[pageNumbers.length - 1]}
            onClick={() => onPageChange(currentPage + 1)}
          >
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default PostTable;
