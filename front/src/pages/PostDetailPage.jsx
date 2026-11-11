import { useEffect, useRef, useState } from 'react';
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
import PostCategoryBadge from '../components/post/PostCategoryBadge';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import ReportModal from '../components/common/ReportModal';
import useAuthStore from '../store/authStore';
import { createReport } from '../api/reports';

const COMMENTS_PER_PAGE = 20;
const COMMENT_PAGE_BUTTON_COUNT = 5;

// AI작성: 일반 공백 외 제로폭·형식 공백만 붙여넣은 경우에도 댓글 내용을 비어 있는 값으로 처리합니다.
const normalizeCommentContent = (content) => {
  const trimmedContent = content.trim();

  return /^[\u200B-\u200D\u2060\uFEFF\u180E]*$/u.test(trimmedContent)
    ? ''
    : trimmedContent;
};

// AI작성: 로그인 JWT의 subject는 백엔드가 발급한 회원 번호이므로 댓글 작성 권한 비교에 사용한다.
const getMemberIdFromToken = (token) => {
  try {
    const payload = String(token || '').split('.')[1];

    if (!payload) return null;

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decodedPayload = window.atob(
      normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '='),
    );
    const memberId = JSON.parse(decodedPayload).sub;

    return memberId ? String(memberId) : null;
  } catch {
    return null;
  }
};

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

const TitleRow = styled.div`
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
`;

const Title = styled.h1`
  margin: 0;
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
  const token = useAuthStore((state) => state.token);
  const memberId = getMemberIdFromToken(token);
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
  const [reportTarget, setReportTarget] = useState(null);
  const [reportErrorMessage, setReportErrorMessage] = useState('');
  const [isSubmittingReport, setIsSubmittingReport] = useState(false);
  const [reportNotice, setReportNotice] = useState('');
  const postDetailRequestRef = useRef(null);
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
        // AI작성: 개발 모드의 중복 Effect가 같은 상세 API를 두 번 호출하지 않도록 진행 중인 요청을 공유한다.
        let request = postDetailRequestRef.current;
        if (!request || request.postId !== id) {
          request = { postId: id, promise: getPostDetail(id) };
          postDetailRequestRef.current = request;
        }

        // AI작성: 상세 API 응답을 기존 PostDetail 컴포넌트의 표시 형식으로 변환합니다.
        const response = await request.promise;
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
          // AI작성: 상세 API의 표시 순서대로 첨부를 전달합니다.
          images: detail.images || [],
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
        if (postDetailRequestRef.current?.postId === id) {
          postDetailRequestRef.current = null;
        }
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

  // AI작성: 숫자 메뉴를 누르면 누적하지 않고 선택한 페이지의 댓글 목록만 표시합니다.
  const handleCommentPageChange = async (page) => {
    if (isLoadingComments || page < 1 || page > commentTotalPages || page === commentPage) return;

    setIsLoadingComments(true);
    setCommentErrorMessage('');

    try {
      const response = await getComments(id, { page, size: COMMENTS_PER_PAGE });
      if (!response.success) {
        throw new Error(response.error?.message || '댓글을 불러오지 못했습니다.');
      }

      setComments((response.data?.comments || []).map(mapComment));
      setCommentPage(response.meta?.page || page);
      setCommentTotal(response.data?.totalCommentCount || 0);
      setCommentTotalPages(response.meta?.totalPages || 0);
    } catch (error) {
      setCommentErrorMessage(
        error.response?.data?.error?.message || error.message || '댓글을 불러오지 못했습니다.'
      );
    } finally {
      setIsLoadingComments(false);
    }
  };

  const commentPageStart = Math.floor((commentPage - 1) / COMMENT_PAGE_BUTTON_COUNT) * COMMENT_PAGE_BUTTON_COUNT + 1;
  const commentPageNumbers = Array.from(
    { length: Math.min(COMMENT_PAGE_BUTTON_COUNT, Math.max(0, commentTotalPages - commentPageStart + 1)) },
    (_, index) => commentPageStart + index,
  );

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

  // AI작성: 새 원댓글은 마지막 페이지에서 보이도록 서버의 최신 페이지 정보를 기준으로 다시 조회합니다.
  const refreshComments = async ({ moveToLastPage = false } = {}) => {
    const firstPageResponse = await getComments(id, { page: 1, size: COMMENTS_PER_PAGE });
    if (!firstPageResponse.success) {
      throw new Error(firstPageResponse.error?.message || '최신 댓글을 불러오지 못했습니다.');
    }

    const lastPage = Math.max(1, firstPageResponse.meta?.totalPages || 1);
    const response = moveToLastPage && lastPage > 1
      ? await getComments(id, { page: lastPage, size: COMMENTS_PER_PAGE })
      : firstPageResponse;

    if (!response.success) {
      throw new Error(response.error?.message || '최신 댓글을 불러오지 못했습니다.');
    }

    setComments((response.data?.comments || []).map(mapComment));
    setCommentPage(response.meta?.page || 1);
    setCommentTotal(response.data?.totalCommentCount || 0);
    setCommentTotalPages(response.meta?.totalPages || 0);
  };

  const handleCommentSubmit = async (event) => {
    event.preventDefault();
    if (!token) {
      goToLogin();
      return;
    }
    const trimmedComment = normalizeCommentContent(commentText);
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

      setCommentText('');
      if (commentDraftKey) sessionStorage.removeItem(commentDraftKey);

      try {
        // AI작성: 현재 페이지에 임시 댓글을 붙이지 않고 마지막 페이지의 서버 결과만 표시합니다.
        setIsLoadingComments(true);
        await refreshComments({ moveToLastPage: true });
      } catch (refreshError) {
        setCommentErrorMessage(
          refreshError.response?.data?.error?.message ||
            refreshError.message ||
            '댓글은 등록되었지만 최신 목록을 불러오지 못했습니다.'
        );
      } finally {
        setIsLoadingComments(false);
      }
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
    const trimmedReply = normalizeCommentContent(replyText);
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

      setReplyingCommentId(null);
      setReplyText('');
      if (replyDraftKey) sessionStorage.removeItem(replyDraftKey);

      try {
        // AI작성: 임시 답글의 작성자 정보가 비어 보이지 않도록 서버 목록을 다시 조회합니다.
        setIsLoadingComments(true);
        await refreshComments();
      } catch (refreshError) {
        setCommentErrorMessage(
          refreshError.response?.data?.error?.message ||
            refreshError.message ||
            '답글은 등록되었지만 최신 목록을 불러오지 못했습니다.'
        );
      } finally {
        setIsLoadingComments(false);
      }
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
    const trimmedComment = normalizeCommentContent(editingCommentText);
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
      // AI작성: 삭제된 댓글은 목록에는 안내 문구로 남지만 댓글 수에서는 즉시 제외한다.
      setCommentTotal((currentTotal) => Math.max(0, currentTotal - 1));
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

  // AI작성: 비로그인 회원은 현재 게시글로 돌아올 수 있도록 로그인 화면으로 이동한다.
  const handleReportStart = (contentReportType, contentTargetId, memberTargetId, contentLabel) => {
    if (!token) {
      goToLogin();
      return;
    }
    setReportErrorMessage('');
    // AI작성: 하나의 신고 모달에서 콘텐츠와 작성자 중 한 대상을 선택할 수 있도록 두 후보를 함께 저장한다.
    setReportTarget({
      targets: [
        { reportType: contentReportType, targetId: contentTargetId, label: `${contentLabel} 신고` },
        { reportType: 'MEMBER', targetId: memberTargetId, label: '작성자 신고' },
      ],
    });
  };

  const handleReportSubmit = async (reason, selectedTarget) => {
    if (!reportTarget || !selectedTarget || isSubmittingReport) return;
    setIsSubmittingReport(true);
    setReportErrorMessage('');
    try {
      await createReport({
        // AI작성: 모달에서 사용자가 선택한 신고 유형과 대상 ID 한 건만 서버로 전송한다.
        reportType: selectedTarget.reportType,
        targetId: selectedTarget.targetId,
        reason,
      });
      setReportTarget(null);
      setReportNotice('신고가 접수되었습니다. 관리자가 내용을 확인합니다.');
    } catch (error) {
      setReportErrorMessage(error.message || '신고를 접수하지 못했습니다.');
    } finally {
      setIsSubmittingReport(false);
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
      <TitleRow>
        <Title>{post.title}</Title>
        <PostCategoryBadge category={post.category} size="detail" />
      </TitleRow>
      <Content>
        {deleteErrorMessage && <ActionError role="alert">{deleteErrorMessage}</ActionError>}
        {commentErrorMessage && <ActionError role="alert">{commentErrorMessage}</ActionError>}
        <PostDetail
          post={post}
          memberId={memberId}
          comments={isLoadingComments ? [] : comments}
          commentTotal={commentTotal}
          isLoadingComments={isLoadingComments}
          commentPage={commentPage}
          commentPageNumbers={commentPageNumbers}
          commentTotalPages={commentTotalPages}
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
          onCommentPageChange={handleCommentPageChange}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onReport={handleReportStart}
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
      {reportTarget && (
        <ReportModal
          targets={reportTarget.targets}
          submitting={isSubmittingReport}
          errorMessage={reportErrorMessage}
          onSubmit={handleReportSubmit}
          onCancel={() => {
            if (!isSubmittingReport) setReportTarget(null);
          }}
        />
      )}
      {reportNotice && (
        <ConfirmModal
          title="신고 접수 완료"
          description={reportNotice}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setReportNotice('')}
          onCancel={() => setReportNotice('')}
        />
      )}
    </Page>
  );
}

export default PostDetailPage;
