import client from './client.js';

function toAdminPostError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminPostError';
  failure.code = responseError?.code || 'ADMIN_POST_REQUEST_FAILED';
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

// AI작성: 관리자 게시글 API 응답을 기존 테이블 표시 형식으로 변환한다.
export async function getAdminPosts({ keyword, page = 1, size = 10, signal } = {}) {
  try {
    const response = await client.get('/admin/posts', {
      params: {
        keyword: keyword || undefined,
        page,
        size,
      },
      signal,
    });

    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new Error('게시글 목록 응답을 확인할 수 없습니다.');
    }

    return {
      items: response.data.data.map((post) => ({
        id: post.postId,
        category: post.categoryName || '분류 없음',
        title: post.title,
        author: post.writerNickname || '알 수 없음',
        date: formatDate(post.createdAt || post.createdAtStr),
      })),
      meta: response.data.meta,
    };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminPostError(error, '게시글 목록을 불러오지 못했습니다.');
  }
}

// AI작성: 관리자 권한으로 게시글을 소프트 삭제한 뒤 목록을 다시 조회한다.
export async function deleteAdminPost(postId) {
  try {
    const response = await client.delete(`/admin/posts/${encodeURIComponent(postId)}`);
    if (!response.data?.success) {
      throw new Error('게시글 삭제 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminPostError(error, '게시글을 삭제하지 못했습니다.');
  }
}
