import client from './client.js';

function toAdminCommentError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminCommentError';
  failure.code = responseError?.code || 'ADMIN_COMMENT_LIST_FAILED';
  return failure;
}

function formatDate(value) {
  if (!value) return '날짜 확인 필요';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '날짜 확인 필요';
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
    .format(date)
    .replace(/\.\s?/g, '.')
    .replace(/\.$/, '');
}

function formatCommentStatus(status) {
  // AI작성: 정의되지 않은 상태를 공개로 오인하지 않도록 확인 필요로 표시한다.
  if (status === 'VISIBLE') return '공개';
  if (status === 'DELETED') return '삭제';
  return '확인 필요';
}

// AI작성: 관리자 전체 댓글 API 응답을 기존 테이블 표시 형식으로 변환한다.
export async function getAdminComments({ keyword, regionName, status, page = 1, size = 10, signal } = {}) {
  try {
    const response = await client.get('/admin/comments', {
      params: {
        keyword: keyword || undefined,
        regionName: regionName || undefined,
        status: status || undefined,
        page,
        size,
      },
      signal,
    });

    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new Error('댓글 목록 응답을 확인할 수 없습니다.');
    }

    return {
      items: response.data.data.map((comment) => ({
        id: comment.commentId,
        postId: comment.postId,
        content: comment.content,
        author: comment.writerNickname,
        postTitle: comment.postTitle,
        region: comment.regionName,
        status: formatCommentStatus(comment.status),
        date: formatDate(comment.createdAt),
      })),
      meta: response.data.meta,
    };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminCommentError(error, '댓글 목록을 불러오지 못했습니다.');
  }
}

// AI작성: 목록에서 받은 게시글·댓글 번호로 기존 관리자 소프트 삭제 API를 호출한다.
export async function deleteAdminComment(postId, commentId) {
  try {
    const response = await client.delete(
      `/admin/posts/${encodeURIComponent(postId)}/comments/${encodeURIComponent(commentId)}`,
    );

    if (!response.data?.success) {
      throw new Error('댓글 삭제 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminCommentError(error, '댓글을 삭제하지 못했습니다.');
  }
}
