import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import {
  createComment,
  createReply,
  deleteComment,
  deletePost,
  getComments,
  getPostDetail,
  updateComment,
} from '../api/postApi';
import PostDetail from '../components/post/PostDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import useAuthStore from '../store/authStore';

const COMMENTS_PER_PAGE = 20;

// AI작성: 더보기로 가져온 과거 댓글과 방금 작성한 댓글이 시간 순서대로 표시되도록 정렬합니다.
const sortCommentsByCreatedAt = (comments) =>
  [...comments].sort((first, second) => {
    const firstTime = Date.parse(first.createdAt || '');
    const secondTime = Date.parse(second.createdAt || '');

    if (Number.isFinite(firstTime) && Number.isFinite(secondTime) && firstTime !== secondTime) {
      return firstTime - secondTime;
    }

    return Number(first.id) - Number(second.id);
  });

// AI작성: 백엔드 댓글 응답을 화면 컴포넌트의 표시 형식으로 변환합니다.
const mapComment = (comment) => ({
  id: comment.commentId,
  memberId: comment.memberId,
  author: comment.writerNickname || '알 수 없음',
  content: comment.content,
  createdAt: comment.createdAt,
  deleted: Boolean(comment.deleted),
  // AI작성: 백엔드가 반환한 한 단계 대댓글을 화면 표시 형식으로 변환합니다.
  replies: (comment.replies || []).map((reply) => ({
    id: reply.commentId,
    memberId: reply.memberId,
    author: reply.writerNickname || '알 수 없음',
    content: reply.content,
    createdAt: reply.createdAt,
    deleted: Boolean(reply.deleted),
  })),
});

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 20px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Title = styled.h1`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -1.2px;
  line-height: 1.35;
`;

const Content = styled.main`
  margin-top: 14px;
`;

const ActionError = styled.p`
  margin: 0 0 12px;
  color: ${({ theme }) => theme.colors.danger || '#c62828'};
  font-size: 13px;
`;

const EmptyCard = styled.section`
  padding: 42px 20px;
  text-align: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};

  p {
    margin-bottom: 16px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 13px;
  }
`;

function PostDetailPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const memberId = useAuthStore((state) => state.memberId);
  const token = useAuthStore((state) => state.token);
  const [commentText, setCommentText] = useState('');
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deletingCommentId, setDeletingCommentId] = useState(null);
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentPage, setCommentPage] = useState(1);
  const [commentTotal, setCommentTotal] = useState(0);
  const [commentTotalPages, setCommentTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingComments, setIsLoadingComments] = useState(true);
  const [isLoadingMoreComments, setIsLoadingMoreComments] = useState(false);
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);
  const [replyingCommentId, setReplyingCommentId] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [isSubmittingReply, setIsSubmittingReply] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState('');
  const [isUpdatingComment, setIsUpdatingComment] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isDeletingComment, setIsDeletingComment] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [commentErrorMessage, setCommentErrorMessage] = useState('');
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');
  const commentDraftKey = memberId && id ? `moa-comment-draft:${memberId}:${id}` : null;
  const replyDraftKey = memberId && id ? `moa-reply-draft:${memberId}:${id}` : null;

  // AI작성: 같은 회원이 같은 게시글에서 작성하던 댓글과 대댓글을 새로고침 후 복원합니다.
  useEffect(() => {
    if (!commentDraftKey || !replyDraftKey) return;

    // oxlint-disable-next-line react/set-state-in-effect -- 외부 sessionStorage 값을 화면 상태로 복원해야 합니다.
    setCommentText(sessionStorage.getItem(commentDraftKey) || '');
    setReplyingCommentId(null);
    setReplyText('');

    try {
      const savedReply = JSON.parse(sessionStorage.getItem(replyDraftKey) || 'null');
      const parentCommentId = Number(savedReply?.parentCommentId);

      if (Number.isInteger(parentCommentId) && parentCommentId > 0 && typeof savedReply?.content === 'string') {
        setReplyingCommentId(parentCommentId);
        setReplyText(savedReply.content);
      } else if (savedReply !== null) {
        sessionStorage.removeItem(replyDraftKey);
      }
    } catch {
      // AI작성: 손상된 임시저장 값은 복원하지 않고 제거해 상세 화면 이용을 방해하지 않도록 합니다.
      sessionStorage.removeItem(replyDraftKey);
    }
  }, [commentDraftKey, replyDraftKey]);

  const handleCommentChange = (value) => {
    setCommentText(value);
    if (!commentDraftKey) return;

    if (value) sessionStorage.setItem(commentDraftKey, value);
    else sessionStorage.removeItem(commentDraftKey);
  };

  const handleReplyChange = (value) => {
    setReplyText(value);
    if (!replyDraftKey || !replyingCommentId) return;

    if (value) {
      sessionStorage.setItem(replyDraftKey, JSON.stringify({
        parentCommentId: replyingCommentId,
        content: value,
      }));
    } else {
      sessionStorage.removeItem(replyDraftKey);
    }
  };

  // AI작성: 인증이 필요한 동작에서 현재 게시글로 다시 돌아올 수 있도록 로그인 이동을 공통 처리한다.
  const goToLogin = () => {
    navigate(`/member/login?redirect=${encodeURIComponent(location.pathname)}`);
  };

  useEffect(() => {
    let isActive = true;

    async function loadPostDetail() {
      setIsLoading(true);
      setErrorMessage('');

      try {
        // AI작성: 상세 API 응답을 기존 PostDetail 컴포넌트의 표시 형식으로 변환합니다.
        const response = await getPostDetail(id);
        if (!response.success) {
          throw new Error(response.error?.message || '게시글을 불러오지 못했습니다.');
        }

        if (!isActive) return;

        const detail = response.data;
        setPost({
          id: detail.postId,
          category: detail.categoryName || '기타',
          title: detail.title,
          content: detail.content,
          author: detail.writerNickname || '알 수 없음',
          writerId: detail.writerId,
          date: detail.createdAtStr || '',
          views: detail.viewCount,
        });
      } catch (error) {
        if (!isActive) return;
        setPost(null);
        setErrorMessage(
          error.response?.data?.error?.message || error.message || '게시글을 불러오지 못했습니다.'
        );
      } finally {
        if (isActive) setIsLoading(false);
      }
    }

    loadPostDetail();

    return () => {
      isActive = false;
    };
  }, [id]);

  useEffect(() => {
    let isActive = true;

    async function loadComments() {
      setIsLoadingComments(true);
      setCommentErrorMessage('');

      try {
        const response = await getComments(id, { page: 1, size: COMMENTS_PER_PAGE });
        if (!response.success) {
          throw new Error(response.error?.message || '댓글을 불러오지 못했습니다.');
        }

        if (!isActive) return;

        setComments((response.data?.comments || []).map(mapComment));
        setCommentPage(response.meta?.page || 1);
        // AI작성: 댓글 개수에는 원댓글과 대댓글을 모두 포함합니다.
        setCommentTotal(response.data?.totalCommentCount || 0);
        setCommentTotalPages(response.meta?.totalPages || 0);
      } catch (error) {
        if (!isActive) return;
        // AI작성: 댓글 조회 실패 시 이전 게시글의 목록과 페이징 정보가 남지 않도록 초기화합니다.
        setComments([]);
        setCommentPage(1);
        setCommentTotal(0);
        setCommentTotalPages(0);
        setCommentErrorMessage(
          error.response?.data?.error?.message || error.message || '댓글을 불러오지 못했습니다.'
        );
      } finally {
        if (isActive) setIsLoadingComments(false);
      }
    }

    loadComments();

    return () => {
      isActive = false;
    };
  }, [id]);

  const handleLoadMoreComments = async () => {
    if (isLoadingMoreComments || commentPage >= commentTotalPages) return;

    setIsLoadingMoreComments(true);
    setCommentErrorMessage('');

    try {
      const nextPage = commentPage + 1;
      const response = await getComments(id, { page: nextPage, size: COMMENTS_PER_PAGE });
      if (!response.success) {
        throw new Error(response.error?.message || '댓글을 더 불러오지 못했습니다.');
      }

      // AI작성: 댓글 더보기는 읽던 흐름을 유지하고 모바일에서도 쉽게 사용하도록 기존 목록 뒤에 추가합니다.
      setComments((currentComments) => {
        const currentCommentIds = new Set(currentComments.map((comment) => comment.id));
        const newComments = (response.data?.comments || [])
          .map(mapComment)
          .filter((comment) => !currentCommentIds.has(comment.id));

        // AI작성: 새 댓글을 먼저 화면에 추가한 상태에서도 다음 페이지와 합친 뒤 작성 시간순을 유지합니다.
        return sortCommentsByCreatedAt([...currentComments, ...newComments]);
      });
      setCommentPage(response.meta?.page || nextPage);
      setCommentTotal(response.data?.totalCommentCount ?? commentTotal);
      setCommentTotalPages(response.meta?.totalPages || commentTotalPages);
    } catch (error) {
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '댓글을 더 불러오지 못했습니다.'
      );
    } finally {
      setIsLoadingMoreComments(false);
    }
  };

  const handleEdit = () => {
    navigate(`/post/${id}/edit`);
  };

  const handleDelete = () => {
    setDeleteErrorMessage('');
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (isDeleting) return;

    setIsDeleting(true);
    setDeleteErrorMessage('');

    try {
      const response = await deletePost(id);
      if (!response.success) {
        throw new Error(response.error?.message || '게시글 삭제에 실패했습니다.');
      }

      navigate('/post/list');
    } catch (error) {
      setIsDeleteModalOpen(false);
      setDeleteErrorMessage(
        error.response?.data?.error?.message || error.message || '게시글 삭제에 실패했습니다.'
      );
    } finally {
      setIsDeleting(false);
    }
  };

  const handleCommentSubmit = async (event) => {
    event.preventDefault();
    if (!token) {
      goToLogin();
      return;
    }
    const trimmedComment = commentText.trim();
    if (!trimmedComment || isSubmittingComment) return;

    setIsSubmittingComment(true);
    setCommentErrorMessage('');

    try {
      const response = await createComment(id, {
        content: trimmedComment,
      });

      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '댓글 등록에 실패했습니다.');
      }

      setComments((currentComments) => [
        ...currentComments,
        {
          id: response.data.commentId,
          // AI작성: 등록 직후에도 본인 댓글의 수정·삭제 버튼이 표시되도록 회원 번호를 유지합니다.
          memberId: response.data.memberId,
          author: '나',
          content: response.data.content,
          createdAt: response.data.createdAt,
          replies: [],
        },
      ]);
      setCommentText('');
      if (commentDraftKey) sessionStorage.removeItem(commentDraftKey);
      setCommentTotal((currentTotal) => currentTotal + 1);
    } catch (error) {
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '댓글 등록에 실패했습니다.'
      );
    } finally {
      setIsSubmittingComment(false);
    }
  };

  const handleCommentEditStart = (comment) => {
    setCommentErrorMessage('');
    setEditingCommentId(comment.id);
    setEditingCommentText(comment.content);
  };

  const handleReplyStart = (commentId) => {
    if (!token) {
      goToLogin();
      return;
    }
    setCommentErrorMessage('');
    setReplyingCommentId(commentId);
    setReplyText('');
    if (replyDraftKey) sessionStorage.removeItem(replyDraftKey);
  };

  const handleReplyCancel = () => {
    if (isSubmittingReply) return;
    setReplyingCommentId(null);
    setReplyText('');
    if (replyDraftKey) sessionStorage.removeItem(replyDraftKey);
  };

  const handleReplySubmit = async (event) => {
    event.preventDefault();
    const trimmedReply = replyText.trim();
    if (!replyingCommentId || !trimmedReply || isSubmittingReply) return;

    setIsSubmittingReply(true);
    setCommentErrorMessage('');

    try {
      const response = await createReply(id, replyingCommentId, {
        content: trimmedReply,
      });
      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '답글 등록에 실패했습니다.');
      }

      // AI작성: 등록된 대댓글을 해당 원댓글의 replies 배열 끝에 추가합니다.
      setComments((currentComments) =>
        currentComments.map((comment) =>
          comment.id === replyingCommentId
            ? {
                ...comment,
                replies: [
                  ...comment.replies,
                  {
                    id: response.data.commentId,
                    // AI작성: 등록 직후에도 본인 대댓글의 수정·삭제 버튼이 표시되도록 회원 번호를 유지합니다.
                    memberId: response.data.memberId,
                    author: '나',
                    content: response.data.content,
                    createdAt: response.data.createdAt,
                    deleted: false,
                  },
                ],
              }
            : comment
        )
      );
      setReplyingCommentId(null);
      setReplyText('');
      if (replyDraftKey) sessionStorage.removeItem(replyDraftKey);
      setCommentTotal((currentTotal) => currentTotal + 1);
    } catch (error) {
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '답글 등록에 실패했습니다.'
      );
    } finally {
      setIsSubmittingReply(false);
    }
  };

  const handleCommentEditCancel = () => {
    if (isUpdatingComment) return;
    setEditingCommentId(null);
    setEditingCommentText('');
  };

  const handleCommentEditSubmit = async (event) => {
    event.preventDefault();
    if (!token) {
      goToLogin();
      return;
    }
    const trimmedComment = editingCommentText.trim();
    if (!editingCommentId || !trimmedComment || isUpdatingComment) return;

    setIsUpdatingComment(true);
    setCommentErrorMessage('');

    try {
      // TODO AI작성: 로그인 연동 후 백엔드에서 로그인 회원과 댓글 작성자의 수정 권한을 검증해야 합니다.
      const response = await updateComment(id, editingCommentId, { content: trimmedComment });
      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '댓글 수정에 실패했습니다.');
      }

      setComments((currentComments) =>
        currentComments.map((comment) => {
          if (comment.id === editingCommentId) {
            return { ...comment, content: response.data.content };
          }

          // AI작성: 수정 대상이 대댓글이면 해당 원댓글의 replies 배열에서 내용을 갱신합니다.
          return {
            ...comment,
            replies: comment.replies.map((reply) =>
              reply.id === editingCommentId ? { ...reply, content: response.data.content } : reply
            ),
          };
        })
      );
      setEditingCommentId(null);
      setEditingCommentText('');
    } catch (error) {
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '댓글 수정에 실패했습니다.'
      );
    } finally {
      setIsUpdatingComment(false);
    }
  };

  const handleCommentDelete = (commentId) => {
    setCommentErrorMessage('');
    setDeletingCommentId(commentId);
  };

  const handleCommentDeleteConfirm = async () => {
    if (!token) {
      goToLogin();
      return;
    }
    if (!deletingCommentId || isDeletingComment) return;

    setIsDeletingComment(true);
    setCommentErrorMessage('');

    try {
      // TODO AI작성: 로그인 연동 후 백엔드에서 로그인 회원과 댓글 작성자의 삭제 권한을 검증해야 합니다.
      const response = await deleteComment(id, deletingCommentId);
      if (!response.success) {
        throw new Error(response.error?.message || '댓글 삭제에 실패했습니다.');
      }

      // AI작성: 백엔드 소프트 삭제 정책에 맞춰 위치를 유지하고 안내 문구로 바꿉니다.
      setComments((currentComments) =>
        currentComments.map((comment) => {
          if (comment.id === deletingCommentId) {
            return { ...comment, content: '삭제된 댓글입니다.', deleted: true };
          }

          return {
            ...comment,
            replies: comment.replies.map((reply) =>
              reply.id === deletingCommentId
                ? { ...reply, content: '삭제된 댓글입니다.', deleted: true }
                : reply
            ),
          };
        })
      );
      // AI작성: 입력 또는 수정 중인 댓글이 삭제되면 남아 있는 입력 상태를 함께 정리합니다.
      if (replyingCommentId === deletingCommentId) {
        setReplyingCommentId(null);
        setReplyText('');
        if (replyDraftKey) sessionStorage.removeItem(replyDraftKey);
      }
      if (editingCommentId === deletingCommentId) {
        setEditingCommentId(null);
        setEditingCommentText('');
      }
      setDeletingCommentId(null);
    } catch (error) {
      setDeletingCommentId(null);
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '댓글 삭제에 실패했습니다.'
      );
    } finally {
      setIsDeletingComment(false);
    }
  };

  if (isLoading || !post) {
    return (
      <Page>
        <EmptyCard>
          <p>{isLoading ? '게시글을 불러오는 중입니다.' : errorMessage || '존재하지 않거나 삭제된 게시글입니다.'}</p>
          {!isLoading && (
            <Button type="button" $variant="secondary" onClick={() => navigate('/post/list')}>
              목록으로
            </Button>
          )}
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Breadcrumb>게시판 &gt; 게시글 상세</Breadcrumb>
      <Title>{post.title}</Title>
      <Content>
        {deleteErrorMessage && <ActionError role="alert">{deleteErrorMessage}</ActionError>}
        {commentErrorMessage && <ActionError role="alert">{commentErrorMessage}</ActionError>}
        <PostDetail
          post={post}
          memberId={memberId}
          comments={isLoadingComments ? [] : comments}
          commentTotal={commentTotal}
          hasMoreComments={commentPage < commentTotalPages}
          isLoadingMoreComments={isLoadingMoreComments}
          isSubmittingComment={isSubmittingComment}
          replyingCommentId={replyingCommentId}
          replyText={replyText}
          isSubmittingReply={isSubmittingReply}
          editingCommentId={editingCommentId}
          editingCommentText={editingCommentText}
          isUpdatingComment={isUpdatingComment}
          commentText={commentText}
          onCommentChange={handleCommentChange}
          onCommentSubmit={handleCommentSubmit}
          onReplyStart={handleReplyStart}
          onReplyChange={handleReplyChange}
          onReplySubmit={handleReplySubmit}
          onReplyCancel={handleReplyCancel}
          onCommentEditStart={handleCommentEditStart}
          onCommentEditChange={setEditingCommentText}
          onCommentEditSubmit={handleCommentEditSubmit}
          onCommentEditCancel={handleCommentEditCancel}
          onCommentDelete={handleCommentDelete}
          onLoadMoreComments={handleLoadMoreComments}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onBack={() => navigate('/post/list')}
        />
      </Content>
      {isDeleteModalOpen && (
        <ConfirmModal
          title="게시글 삭제"
          description="게시글을 삭제하시겠습니까? 삭제 후에는 목록에서 확인할 수 없습니다."
          confirmLabel={isDeleting ? '삭제 중...' : '게시글 삭제'}
          confirmVariant="danger"
          size="wide"
          cancelFirst
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            if (!isDeleting) setIsDeleteModalOpen(false);
          }}
        />
      )}
      {deletingCommentId && (
        <ConfirmModal
          title="댓글 삭제"
          description="댓글을 삭제하시겠습니까? 삭제된 댓글은 내용만 숨겨집니다."
          confirmLabel={isDeletingComment ? '삭제 중...' : '댓글 삭제'}
          confirmVariant="danger"
          size="wide"
          cancelFirst
          onConfirm={handleCommentDeleteConfirm}
          onCancel={() => {
            if (!isDeletingComment) setDeletingCommentId(null);
          }}
        />
      )}
    </Page>
  );
}

export default PostDetailPage;
