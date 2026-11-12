import { useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';

const REPLIES_PER_LOAD = 5;

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

// AI작성: 상세 본문 아래에 첨부를 원본 비율로 표시합니다.
const PostImages = styled.ul`
  display: grid;
  gap: 12px;
  padding: 0;
  margin: 12px 0;
  list-style: none;

  img {
    display: block;
    max-width: 100%;
    max-height: 640px;
    object-fit: contain;
  }
`;

function PostAttachment({ imageUrl, index }) {
  const [failed, setFailed] = useState(false);
  return (
    <li>
      {failed ? (
        <p>첨부 사진을 불러오지 못했습니다.</p>
      ) : (
        <img src={imageUrl} alt={`첨부 사진 ${index + 1}`} loading="lazy" onError={() => setFailed(true)} />
      )}
    </li>
  );
}

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

// AI작성: 긴 게시글과 댓글 사이를 빠르게 이동할 수 있도록 본문 옆에 고정 네비게이터를 배치한다.
const DetailLayout = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) 108px;
  align-items: start;
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
`;

const DetailBody = styled.div`
  min-width: 0;
`;

const FloatingNavigator = styled.nav`
  position: sticky;
  top: 104px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    position: static;
    width: 100%;
    flex-direction: row;
    justify-content: center;
    padding: 8px;
  }
`;

const NavigatorButton = styled.button`
  min-height: 48px;
  padding: 7px 5px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid transparent;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
  font-weight: 800;
  line-height: 1.3;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: ${({ theme }) => theme.colors.white};
    background: ${({ theme }) => theme.colors.primary};
    outline: none;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    flex: 1;
    min-height: 42px;
    padding: 7px 10px;
  }
`;

const ScrollAnchor = styled.div`
  height: 1px;
  scroll-margin-top: 104px;
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

const CommentsLoading = styled.p`
  margin-top: 14px;
  padding: 20px 0;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 14px;
  text-align: center;
`;

const CommentList = styled.div`
  margin-top: 10px;
`;

const CommentPagination = styled.nav`
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 4px;
`;

const CommentPageButton = styled(Button)`
  min-width: 30px;
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

const AuthorInfo = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
`;

const CommentDate = styled.time`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
`;

const CommentText = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 15px;
  line-height: 1.6;
  /* AI작성: 댓글·대댓글에 저장된 줄바꿈을 유지하고 긴 문자열은 영역 안에서 줄바꿈한다. */
  white-space: pre-wrap;
  overflow-wrap: anywhere;
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

const ReplyMoreButton = styled(TextAction)`
  margin: 12px 0 0 28px;
  font-size: 13px;
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
  height: 76px;
  padding: 10px 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 16px;
  line-height: 1.6;
  resize: none;
  overflow-y: auto;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  /* AI작성: 비로그인 상태의 댓글 입력창은 회색으로 비활성 상태를 분명히 표시합니다. */
  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: ${({ theme }) => theme.colors.background};
    border-color: ${({ theme }) => theme.colors.border};
    cursor: not-allowed;
  }

  &:disabled::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }
`;

// AI작성: 댓글 입력창 높이만큼 버튼이 늘어나지 않도록 하단에 고정된 크기로 표시한다.
const CommentSubmitButton = styled(Button)`
  width: 76px;
  min-width: 76px;
  align-self: stretch;

  /* AI작성: 로그인 안내용 등록 버튼은 회색으로 보이지만 클릭하면 로그인 화면으로 이동합니다. */
  ${({ $loginRequired, theme }) => $loginRequired && `
    color: ${theme.colors.muted};
    background: ${theme.colors.background};
    border-color: ${theme.colors.border};
    cursor: pointer;

    &:hover {
      background: ${theme.colors.border};
      border-color: ${theme.colors.border};
    }
  `}
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

// AI작성: 댓글과 대댓글 작성일을 동일한 한국어 날짜·시간 형식으로 표시한다.
function formatCommentDate(value) {
  const date = new Date(value);
  if (!value || Number.isNaN(date.getTime())) return '-';

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

function PostDetail({
  post,
  memberId,
  comments,
  commentTotal,
  isLoadingComments,
  commentPage,
  commentPageNumbers,
  commentTotalPages,
  isSubmittingComment,
  replyingCommentId,
  replyText,
  isSubmittingReply,
  newReplyParentId,
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
  onReplyMoreViewed,
  onCommentEditStart,
  onCommentEditChange,
  onCommentEditSubmit,
  onCommentEditCancel,
  onCommentDelete,
  onCommentPageChange,
  onEdit,
  onDelete,
  onReport,
  onBack,
}) {
  const pageTopRef = useRef(null);
  const contentEndRef = useRef(null);
  const pageBottomRef = useRef(null);

  // AI작성: 버튼 선택 시 긴 화면 안에서 목적 위치로 부드럽게 이동한다.
  const moveTo = (targetRef) => {
    targetRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }; // AI작성: 스크롤 이동 함수의 범위를 닫아 이후 댓글 상태가 컴포넌트 범위에 위치하도록 합니다.

  const [visibleReplyCounts, setVisibleReplyCounts] = useState({});
  const getVisibleReplyCount = (commentId) => visibleReplyCounts[commentId] || REPLIES_PER_LOAD;

  const handleReplyMore = (commentId) => {
    // AI작성: 숫자 페이지 대신 같은 부모 댓글 아래 답글을 5개씩 추가로 표시합니다.
    setVisibleReplyCounts((currentCounts) => ({
      ...currentCounts,
      [commentId]: (currentCounts[commentId] || REPLIES_PER_LOAD) + REPLIES_PER_LOAD,
    }));
    onReplyMoreViewed(commentId);
  };

  return (
    <DetailLayout>
      <DetailBody>
      <ScrollAnchor ref={pageTopRef} />
      <ArticleCard>
        <Meta>
          {post.author} · {post.date} · 조회 {post.views}
        </Meta>
        <PostContent>{post.content}</PostContent>
        {post.images?.length > 0 && (
          <PostImages aria-label="첨부 사진">
            {post.images.map((image, index) => (
              <PostAttachment key={image.imageId} imageUrl={image.imageUrl} index={index} />
            ))}
          </PostImages>
        )}
        <ScrollAnchor ref={contentEndRef} />
        <PostActions>
          {Number(post.writerId) === Number(memberId) ? <>
            <Button type="button" $size="small" onClick={onEdit}>수정</Button>
            <Button type="button" $variant="danger" $size="small" onClick={onDelete}>삭제</Button>
          </> : memberId && (
            /* AI작성: 비로그인 상태에서는 신고 기능 진입 버튼을 표시하지 않습니다. */
            <Button type="button" $variant="danger" $size="small" onClick={() => onReport('POST', post.id, post.writerId, '게시글')}>
              신고하기
            </Button>
          )}
        </PostActions>
      </ArticleCard>
      <ListActions>
        <Button type="button" $variant="secondary" $size="small" onClick={onBack}>
          목록으로
        </Button>
      </ListActions>
      <Comments>
        {isLoadingComments ? (
          // AI작성: 댓글 개수 0개를 먼저 노출하지 않고 조회 완료 후 실제 목록을 표시합니다.
          <CommentsLoading role="status">댓글을 불러오는 중입니다.</CommentsLoading>
        ) : <>
          <CommentsTitle>
            댓글 {commentTotal}개
          </CommentsTitle>
          <CommentList>
          {comments.map((comment) => (
            <Comment key={comment.id}>
              <CommentHeader>
                <AuthorInfo>
                  <Author>{comment.author}</Author>
                  <CommentDate dateTime={comment.createdAt}>{formatCommentDate(comment.createdAt)}</CommentDate>
                </AuthorInfo>
                {!comment.deleted && (
                  <ReplyActions>
                    {/* AI작성: 원댓글에만 한 단계 답글 작성 버튼을 제공합니다. */}
                    <TextAction type="button" onClick={() => onReplyStart(comment.id)}>
                      답글
                    </TextAction>
                    {Number(comment.memberId) === Number(memberId) ? <>
                      <TextAction type="button" onClick={() => onCommentEditStart(comment)}>수정</TextAction>
                      <TextAction type="button" $danger onClick={() => onCommentDelete(comment.id)}>삭제</TextAction>
                    </> : memberId && (
                      /* AI작성: 비로그인 상태에서는 댓글 신고 버튼을 표시하지 않습니다. */
                      <TextAction type="button" $danger onClick={() => onReport('COMMENT', comment.id, comment.memberId, '댓글')}>신고하기</TextAction>
                    )}
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
                    // AI작성: 일반 Enter는 줄바꿈으로 두고 Ctrl/Cmd+Enter로 답글을 등록합니다.
                    onKeyDown={(event) => {
                      if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                        event.preventDefault();
                        onReplySubmit(event);
                      }
                    }}
                    maxLength={500}
                    placeholder="댓글은 ctrl+enter로 등록이 가능합니다."
                    autoFocus
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
              {comment.replies.slice(0, getVisibleReplyCount(comment.id)).map((reply) => (
                <Reply key={reply.id}>
                  <CommentHeader>
                    <AuthorInfo>
                      <Author>↳ {reply.author}</Author>
                      <CommentDate dateTime={reply.createdAt}>{formatCommentDate(reply.createdAt)}</CommentDate>
                    </AuthorInfo>
                    {!reply.deleted && (
                      <ReplyActions>
                        {/* AI작성: 대댓글은 한 단계만 허용하므로 수정과 삭제 기능만 제공합니다. */}
                        {Number(reply.memberId) === Number(memberId) ? <>
                          <TextAction type="button" onClick={() => onCommentEditStart(reply)}>수정</TextAction>
                          <TextAction type="button" $danger onClick={() => onCommentDelete(reply.id)}>삭제</TextAction>
                        </> : memberId && (
                          /* AI작성: 비로그인 상태에서는 답글 신고 버튼을 표시하지 않습니다. */
                          <TextAction type="button" $danger onClick={() => onReport('COMMENT', reply.id, reply.memberId, '댓글')}>신고하기</TextAction>
                        )}
                      </ReplyActions>
                    )}
                  </CommentHeader>
                  {editingCommentId === reply.id ? (
                    <CommentEditForm onSubmit={onCommentEditSubmit}>
                      <CommentInput
                        value={editingCommentText}
                        onChange={(event) => onCommentEditChange(event.target.value)}
                        maxLength={500}
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
              {comment.replies.length > getVisibleReplyCount(comment.id) && (
                <ReplyMoreButton type="button" onClick={() => handleReplyMore(comment.id)}>
                  {newReplyParentId === comment.id
                    ? '새 답글이 등록되었습니다. 답글 더보기'
                    : `답글 더보기 (남은 ${comment.replies.length - getVisibleReplyCount(comment.id)}개)`}
                </ReplyMoreButton>
              )}
            </Comment>
          ))}
          </CommentList>
          {commentTotalPages > 1 && (
          <CommentPagination aria-label="댓글 페이지 이동">
            <CommentPageButton
              type="button"
              $variant="secondary"
              $size="small"
              disabled={commentPage === 1}
              onClick={() => onCommentPageChange(commentPage - 1)}
            >
              이전
            </CommentPageButton>
            {commentPageNumbers.map((page) => (
              <CommentPageButton
                key={page}
                type="button"
                $variant={page === commentPage ? 'primary' : 'secondary'}
                $size="small"
                aria-current={page === commentPage ? 'page' : undefined}
                onClick={() => onCommentPageChange(page)}
              >
                {page}
              </CommentPageButton>
            ))}
            <CommentPageButton
              type="button"
              $variant="secondary"
              $size="small"
              disabled={commentPage === commentTotalPages}
              onClick={() => onCommentPageChange(commentPage + 1)}
            >
              다음
            </CommentPageButton>
          </CommentPagination>
          )}
        </>}
        <CommentForm onSubmit={onCommentSubmit}>
          <CommentInput
            value={commentText}
            onChange={(event) => onCommentChange(event.target.value)}
            // AI작성: 일반 Enter는 줄바꿈으로 남기고 Ctrl/Cmd+Enter만 등록 동작으로 처리합니다.
            onKeyDown={(event) => {
              if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
                event.preventDefault();
                onCommentSubmit(event);
              }
            }}
            maxLength={500}
            // AI작성: 비로그인 회원은 댓글을 작성할 수 없도록 입력 영역을 비활성화합니다.
            disabled={!memberId}
            placeholder={memberId ? '댓글은 ctrl+enter로 등록이 가능합니다.' : '댓글 작성하려면 로그인이 필요합니다.'}
          />
          <CommentSubmitButton
            type="submit"
            $size="small"
            $loginRequired={!memberId}
            disabled={isSubmittingComment}
          >
            {isSubmittingComment ? '등록 중...' : '등록'}
          </CommentSubmitButton>
        </CommentForm>
      </Comments>
      <ScrollAnchor ref={pageBottomRef} />
      </DetailBody>
      <FloatingNavigator aria-label="게시글 위치 이동">
        <NavigatorButton type="button" onClick={() => moveTo(pageTopRef)}>↑ 맨 위</NavigatorButton>
        <NavigatorButton type="button" onClick={() => moveTo(contentEndRef)}>본문 끝</NavigatorButton>
        <NavigatorButton type="button" onClick={() => moveTo(pageBottomRef)}>↓ 맨 아래</NavigatorButton>
      </FloatingNavigator>
    </DetailLayout>
  );
}

export default PostDetail;
