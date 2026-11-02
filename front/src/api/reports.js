import client from './client.js';

// AI작성: 로그인 회원의 게시글·댓글·회원 신고를 공통 API로 접수한다.
export async function createReport({ reportType, targetId, reason }) {
  try {
    const response = await client.post('/reports', { reportType, targetId, reason });
    if (!response.data?.success || !response.data.data) {
      throw new Error('신고 접수 응답을 확인할 수 없습니다.');
    }
    return response.data.data;
  } catch (error) {
    const message = error.response?.data?.error?.message
      || error.message
      || '신고를 접수하지 못했습니다.';
    throw new Error(message);
  }
}
