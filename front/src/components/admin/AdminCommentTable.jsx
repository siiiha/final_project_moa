import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  margin-top: 24px;
  padding: 18px;
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;

const TableWrap = styled.div`
  margin-top: 16px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;

  th {
    padding: 13px 12px;
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 12px;
    font-weight: 700;
    text-align: left;
  }

  td {
    padding: 12px;
    color: ${({ theme }) => theme.colors.secondaryText};
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    font-size: 12px;
    vertical-align: middle;
  }

  tr:last-child td {
    border-bottom: 0;
  }

  th:nth-child(1) {
    width: 30%;
  }
  th:nth-child(2) {
    width: 13%;
  }
  th:nth-child(3) {
    width: 29%;
  }
  th:nth-child(4) {
    width: 11%;
  }
  th:nth-child(5) {
    width: 17%;
    text-align: right;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    thead {
      display: none;
    }

    tbody,
    tr,
    td {
      display: block;
      width: 100%;
    }

    tr {
      padding: 8px 0;
      border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    }

    tr:last-child {
      border-bottom: 0;
    }

    td {
      min-height: 27px;
      padding: 5px 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      border: 0;
      text-align: right;
    }

    td::before {
      flex: 0 0 auto;
      color: ${({ theme }) => theme.colors.muted};
      content: attr(data-label);
      font-weight: 700;
      text-align: left;
    }

    td:last-child::before {
      margin-right: auto;
    }
  }
`;

const EllipsisText = styled.span`
  overflow: hidden;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const PostTitle = styled(EllipsisText)`
  color: ${({ theme }) => theme.colors.secondaryText};
`;

const ActionArea = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 5px;
`;

const ActionButton = styled(Button)`
  min-height: 30px;
  padding: 6px 8px;
  font-size: 10px;
`;

const Pagination = styled.nav`
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 5px;
`;

const PageButton = styled.button`
  min-width: 28px;
  height: 28px;
  padding: 0 7px;
  color: ${({ $active, theme }) => ($active ? theme.colors.white : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
  font-weight: 700;
`;

const Empty = styled.p`
  padding: 46px 16px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 13px;
  text-align: center;
`;

function AdminCommentTable({ comments, totalCount, currentPage, totalPages, onPageChange, onToggleHidden, onDelete }) {
  return (
    <Card>
      <Title>조회 결과 {totalCount}건</Title>
      {comments.length ? (
        <>
          <TableWrap>
            <Table>
              <thead>
                <tr>
                  <th scope="col">댓글 내용</th>
                  <th scope="col">작성자</th>
                  <th scope="col">게시글</th>
                  <th scope="col">작성일</th>
                  <th scope="col">관리</th>
                </tr>
              </thead>
              <tbody>
                {comments.map((comment) => (
                  <tr key={comment.id}>
                    <td data-label="댓글 내용">
                      <EllipsisText>{comment.content}</EllipsisText>
                    </td>
                    <td data-label="작성자">{comment.author}</td>
                    <td data-label="게시글">
                      <PostTitle>{comment.postTitle}</PostTitle>
                    </td>
                    <td data-label="작성일">{comment.date.slice(5)}</td>
                    <td data-label="관리">
                      <ActionArea>
                        <ActionButton type="button" $variant="secondary" onClick={() => onToggleHidden(comment.id)}>
                          {comment.status === '공개' ? '숨김' : '공개'}
                        </ActionButton>
                        <ActionButton type="button" $variant="danger" onClick={() => onDelete(comment.id)}>
                          삭제
                        </ActionButton>
                      </ActionArea>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableWrap>
          {totalPages > 1 && (
            <Pagination aria-label="관리자 댓글 목록 페이지">
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
      ) : (
        <Empty>검색 조건에 맞는 댓글이 없습니다.</Empty>
      )}
    </Card>
  );
}

export default AdminCommentTable;
