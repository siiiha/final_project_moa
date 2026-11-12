import styled from 'styled-components';
import Button from '../common/Button';
import PostCategoryBadge from './PostCategoryBadge';

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

const PostTitle = styled.button`
  width: 100%;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.colors.text};
  background: transparent;
  border: 0;
  font-size: inherit;
  text-align: left;

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    text-decoration: underline;
  }
`;

const PostTitleText = styled.span`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const CommentCount = styled.span`
  flex: 0 0 auto;
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

function PostTable({
  posts,
  totalCount,
  currentPage,
  pageNumbers,
  previousGroupPage,
  nextGroupPage,
  totalPages,
  onPageChange,
  onPostClick,
}) {
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
                      <PostCategoryBadge category={post.category} size="list" />
                    </td>
                    <td>
                      <PostTitle type="button" onClick={() => onPostClick(post.id)}>
                        <PostTitleText>{post.title}</PostTitleText>
                        {post.commentCount > 0 && <CommentCount>[{post.commentCount}]</CommentCount>}
                      </PostTitle>
                    </td>
                    <td>{post.author}</td>
                    {/* AI작성: 게시글 상세·댓글과 동일하게 연도를 포함한 전체 작성일을 표시한다. */}
                    <td>{post.date || '-'}</td>
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
            disabled={previousGroupPage < 1}
            onClick={() => onPageChange(previousGroupPage)}
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
            disabled={nextGroupPage > totalPages}
            onClick={() => onPageChange(nextGroupPage)}
          >
            다음
          </PageButton>
        </Pagination>
      )}
    </>
  );
}

export default PostTable;
