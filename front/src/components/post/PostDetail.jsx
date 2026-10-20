import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const ArticleCard = styled.article`
  padding: 22px 18px 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 18px 14px 12px;
  }
`;

const Meta = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const PostContent = styled.div`
  min-height: 215px;
  margin-top: 16px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-line;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-height: 180px;
    font-size: 12px;
  }
`;

const PostActions = styled.div`
  padding-top: 13px;
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const ListActions = styled.div`
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
`;

const Comments = styled.section`
  margin-top: 14px;
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 16px 14px;
  }
`;

const CommentsTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const CommentList = styled.div`
  margin-top: 10px;
`;

const LoadMoreActions = styled.div`
  margin-top: 14px;
  display: flex;
  justify-content: center;
`;

const Comment = styled.article`
  padding: 12px 0;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
`;

const CommentHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const Author = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 800;
`;

const CommentText = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.6;
`;

const Reply = styled.div`
  margin: 12px 0 0 14px;
  padding: 12px 0 0 14px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
  border-left: 2px solid ${({ theme }) => theme.colors.primarySoft};
`;

const ReplyActions = styled.div`
  display: flex;
  gap: 8px;
`;

const TextAction = styled.button`
  padding: 3px;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.primary)};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
  font-weight: 700;
`;

const CommentForm = styled.form`
  margin-top: 14px;
  padding-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const CommentInput = styled.input`
  width: 100%;
  height: 38px;
  padding: 0 11px;
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
`;

function PostDetail({
  post,
  comments,
  commentTotal,
  hasMoreComments,
  isLoadingMoreComments,
  commentText,
  onCommentChange,
  onCommentSubmit,
  onLoadMoreComments,
  onEdit,
  onDelete,
  onBack,
}) {
  return (
    <>
      <ArticleCard>
        <Badge>{post.category}</Badge>
        <Meta>
          {post.author} · {post.date} · 조회 {post.views}
        </Meta>
        <PostContent>{post.content}</PostContent>
        <PostActions>
          <Button type="button" $size="small" onClick={onEdit}>
            수정
          </Button>
          <Button type="button" $variant="danger" $size="small" onClick={onDelete}>
            삭제
          </Button>
        </PostActions>
      </ArticleCard>
      <ListActions>
        <Button type="button" $variant="secondary" $size="small" onClick={onBack}>
          목록으로
        </Button>
      </ListActions>
      <Comments>
        <CommentsTitle>
          댓글 {commentTotal}개
        </CommentsTitle>
        <CommentList>
          {comments.map((comment) => (
            <Comment key={comment.id}>
              <CommentHeader>
                <Author>{comment.author}</Author>
                <TextAction type="button">답글</TextAction>
              </CommentHeader>
              <CommentText>{comment.content}</CommentText>
              {comment.replies.map((reply) => (
                <Reply key={reply.id}>
                  <CommentHeader>
                    <Author>↳ {reply.author}</Author>
                    <ReplyActions>
                      <TextAction type="button">답글</TextAction>
                      <TextAction type="button">수정</TextAction>
                      <TextAction type="button" $danger>
                        삭제
                      </TextAction>
                    </ReplyActions>
                  </CommentHeader>
                  <CommentText>{reply.content}</CommentText>
                </Reply>
              ))}
            </Comment>
          ))}
        </CommentList>
        {hasMoreComments && (
          <LoadMoreActions>
            {/* AI작성: 숫자 페이징보다 읽던 흐름을 유지하고 모바일에서 누르기 쉬운 더보기 방식을 사용합니다. */}
            <Button
              type="button"
              $variant="secondary"
              $size="small"
              disabled={isLoadingMoreComments}
              onClick={onLoadMoreComments}
            >
              {isLoadingMoreComments ? '댓글 불러오는 중...' : '댓글 더보기'}
            </Button>
          </LoadMoreActions>
        )}
        <CommentForm onSubmit={onCommentSubmit}>
          <CommentInput
            value={commentText}
            onChange={(event) => onCommentChange(event.target.value)}
            placeholder="이웃에게 따뜻한 댓글을 남겨보세요."
          />
          <Button type="submit" $size="small">
            등록
          </Button>
        </CommentForm>
      </Comments>
    </>
  );
}

export default PostDetail;
