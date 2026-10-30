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

// AI작성: 50~60세대 사용자가 상세 정보와 댓글을 편하게 읽도록 작은 글자 크기를 확대합니다.
const Meta = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 14px;
`;

const PostContent = styled.div`
  min-height: 215px;
  margin-top: 16px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 16px;
  line-height: 1.75;
  white-space: pre-line;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-height: 180px;
    font-size: 15px;
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
  font-size: 18px;
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
  font-size: 15px;
  font-weight: 800;
`;

const CommentText = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 15px;
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
  font-size: 14px;
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

// AI작성: 최대 500자의 댓글을 여러 줄로 편하게 작성할 수 있도록 textarea를 사용합니다.
const CommentInput = styled.textarea`
  width: 100%;
  min-height: 76px;
  padding: 10px 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 16px;
  line-height: 1.6;
  resize: vertical;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const CommentEditForm = styled.form`
  margin-top: 8px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr 1fr;

    ${CommentInput} {
      grid-column: 1 / -1;
    }
  }
`;

const ReplyForm = styled(CommentEditForm)`
  margin-left: 14px;
  padding-left: 14px;
  border-left: 2px solid ${({ theme }) => theme.colors.primarySoft};
`;

function PostDetail({
  post,
  memberId,
  comments,
  commentTotal,
  hasMoreComments,
  isLoadingMoreComments,
  isSubmittingComment,
  replyingCommentId,
  replyText,
  isSubmittingReply,
  editingCommentId,
  editingCommentText,
  isUpdatingComment,
  commentText,
  onCommentChange,
  onCommentSubmit,
  onReplyStart,
  onReplyChange,
  onReplySubmit,
  onReplyCancel,
  onCommentEditStart,
  onCommentEditChange,
  onCommentEditSubmit,
  onCommentEditCancel,
  onCommentDelete,
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
        {Number(post.writerId) === Number(memberId) && <PostActions>
          <Button type="button" $size="small" onClick={onEdit}>
            수정
          </Button>
          <Button type="button" $variant="danger" $size="small" onClick={onDelete}>
            삭제
          </Button>
        </PostActions>}
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
                {!comment.deleted && (
                  <ReplyActions>
                    {/* AI작성: 원댓글에만 한 단계 답글 작성 버튼을 제공합니다. */}
                    <TextAction type="button" onClick={() => onReplyStart(comment.id)}>
                      답글
                    </TextAction>
                    {Number(comment.memberId) === Number(memberId) && <>
                      <TextAction type="button" onClick={() => onCommentEditStart(comment)}>수정</TextAction>
                      <TextAction type="button" $danger onClick={() => onCommentDelete(comment.id)}>삭제</TextAction>
                    </>}
                  </ReplyActions>
                )}
              </CommentHeader>
              {editingCommentId === comment.id ? (
                <CommentEditForm onSubmit={onCommentEditSubmit}>
                  {/* AI작성: 기존 내용을 보며 바로 수정할 수 있도록 댓글 아래에 입력창을 표시합니다. */}
                  <CommentInput
                    value={editingCommentText}
                    onChange={(event) => onCommentEditChange(event.target.value)}
                    maxLength={500}
                    required
                  />
                  <Button type="submit" $size="small" disabled={isUpdatingComment}>
                    {isUpdatingComment ? '저장 중...' : '저장'}
                  </Button>
                  <Button
                    type="button"
                    $variant="secondary"
                    $size="small"
                    disabled={isUpdatingComment}
                    onClick={onCommentEditCancel}
                  >
                    취소
                  </Button>
                </CommentEditForm>
              ) : (
                <CommentText>{comment.content}</CommentText>
              )}
              {replyingCommentId === comment.id && (
                <ReplyForm onSubmit={onReplySubmit}>
                  {/* AI작성: 답글 버튼을 누른 원댓글 바로 아래에 대댓글 입력창을 표시합니다. */}
                  <CommentInput
                    value={replyText}
                    onChange={(event) => onReplyChange(event.target.value)}
                    maxLength={500}
                    placeholder="답글을 입력해주세요."
                    autoFocus
                    required
                  />
                  <Button type="submit" $size="small" disabled={isSubmittingReply}>
                    {isSubmittingReply ? '등록 중...' : '등록'}
                  </Button>
                  <Button
                    type="button"
                    $variant="secondary"
                    $size="small"
                    disabled={isSubmittingReply}
                    onClick={onReplyCancel}
                  >
                    취소
                  </Button>
                </ReplyForm>
              )}
              {comment.replies.map((reply) => (
                <Reply key={reply.id}>
                  <CommentHeader>
                    <Author>↳ {reply.author}</Author>
                    {!reply.deleted && (
                      <ReplyActions>
                        {/* AI작성: 대댓글은 한 단계만 허용하므로 수정과 삭제 기능만 제공합니다. */}
                        {Number(reply.memberId) === Number(memberId) && <>
                          <TextAction type="button" onClick={() => onCommentEditStart(reply)}>수정</TextAction>
                          <TextAction type="button" $danger onClick={() => onCommentDelete(reply.id)}>삭제</TextAction>
                        </>}
                      </ReplyActions>
                    )}
                  </CommentHeader>
                  {editingCommentId === reply.id ? (
                    <CommentEditForm onSubmit={onCommentEditSubmit}>
                      <CommentInput
                        value={editingCommentText}
                        onChange={(event) => onCommentEditChange(event.target.value)}
                        maxLength={500}
                        required
                      />
                      <Button type="submit" $size="small" disabled={isUpdatingComment}>
                        {isUpdatingComment ? '저장 중...' : '저장'}
                      </Button>
                      <Button
                        type="button"
                        $variant="secondary"
                        $size="small"
                        disabled={isUpdatingComment}
                        onClick={onCommentEditCancel}
                      >
                        취소
                      </Button>
                    </CommentEditForm>
                  ) : (
                    <CommentText>{reply.content}</CommentText>
                  )}
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
            maxLength={500}
            placeholder="이웃에게 따뜻한 댓글을 남겨보세요."
          />
          <Button type="submit" $size="small" disabled={isSubmittingComment}>
            {isSubmittingComment ? '등록 중...' : '등록'}
          </Button>
        </CommentForm>
      </Comments>
    </>
  );
}

export default PostDetail;
