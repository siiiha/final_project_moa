import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { createComment, deleteComment, deletePost, getComments, getPostDetail, updateComment } from '../api/postApi';
import PostDetail from '../components/post/PostDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';

const COMMENTS_PER_PAGE = 20;

// AI작성: 인증 연동 전에만 사용하는 임시 회원 ID이며, 추후 로그인 회원 ID로 교체해야 합니다.
const TEMP_MEMBER_ID = 44;

// AI작성: 백엔드 댓글 응답을 화면 컴포넌트의 표시 형식으로 변환합니다.
const mapComment = (comment) => ({
  id: comment.commentId,
  author: comment.writerNickname || '알 수 없음',
  content: comment.content,
  replies: [],
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
  const { id } = useParams();
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
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState('');
  const [isUpdatingComment, setIsUpdatingComment] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isDeletingComment, setIsDeletingComment] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [commentErrorMessage, setCommentErrorMessage] = useState('');
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');

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

        setComments((response.data || []).map(mapComment));
        setCommentPage(response.meta?.page || 1);
        setCommentTotal(response.meta?.total || 0);
        setCommentTotalPages(response.meta?.totalPages || 0);
      } catch (error) {
        if (!isActive) return;
        setComments([]);
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
        const newComments = (response.data || [])
          .map(mapComment)
          .filter((comment) => !currentCommentIds.has(comment.id));

        return [...currentComments, ...newComments];
      });
      setCommentPage(response.meta?.page || nextPage);
      setCommentTotal(response.meta?.total || commentTotal);
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
      // TODO AI작성: 로그인 연동 후 백엔드의 임시 회원 ID(44L)를 인증된 회원 ID로 교체해야 합니다.
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
    const trimmedComment = commentText.trim();
    if (!trimmedComment || isSubmittingComment) return;

    setIsSubmittingComment(true);
    setCommentErrorMessage('');

    try {
      // TODO AI작성: 로그인 연동 후 TEMP_MEMBER_ID를 인증된 회원 ID로 교체해야 합니다.
      const response = await createComment(id, {
        memberId: TEMP_MEMBER_ID,
        content: trimmedComment,
      });

      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '댓글 등록에 실패했습니다.');
      }

      setComments((currentComments) => [
        ...currentComments,
        {
          id: response.data.commentId,
          author: '나',
          content: response.data.content,
          replies: [],
        },
      ]);
      setCommentText('');
      setCommentTotal((currentTotal) => currentTotal + 1);
      setCommentTotalPages(Math.ceil((commentTotal + 1) / COMMENTS_PER_PAGE));
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

  const handleCommentEditCancel = () => {
    if (isUpdatingComment) return;
    setEditingCommentId(null);
    setEditingCommentText('');
  };

  const handleCommentEditSubmit = async (event) => {
    event.preventDefault();
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
        currentComments.map((comment) =>
          comment.id === editingCommentId ? { ...comment, content: response.data.content } : comment
        )
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
    if (!deletingCommentId || isDeletingComment) return;

    setIsDeletingComment(true);
    setCommentErrorMessage('');

    try {
      // TODO AI작성: 로그인 연동 후 백엔드에서 로그인 회원과 댓글 작성자의 삭제 권한을 검증해야 합니다.
      const response = await deleteComment(id, deletingCommentId);
      if (!response.success) {
        throw new Error(response.error?.message || '댓글 삭제에 실패했습니다.');
      }

      setComments((currentComments) =>
        currentComments.filter((comment) => comment.id !== deletingCommentId)
      );
      setCommentTotal((currentTotal) => {
        const nextTotal = Math.max(currentTotal - 1, 0);
        setCommentTotalPages(Math.ceil(nextTotal / COMMENTS_PER_PAGE));
        return nextTotal;
      });
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
          comments={isLoadingComments ? [] : comments}
          commentTotal={commentTotal}
          hasMoreComments={commentPage < commentTotalPages}
          isLoadingMoreComments={isLoadingMoreComments}
          isSubmittingComment={isSubmittingComment}
          editingCommentId={editingCommentId}
          editingCommentText={editingCommentText}
          isUpdatingComment={isUpdatingComment}
          commentText={commentText}
          onCommentChange={setCommentText}
          onCommentSubmit={handleCommentSubmit}
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
          description="댓글을 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다."
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
