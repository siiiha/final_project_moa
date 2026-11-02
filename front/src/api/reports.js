import client from './client.js';

// AI작성: 화면마다 신고 유형 문자열을 중복 작성하지 않도록 API 요청 유형을 공통 관리한다.
export const REPORT_TYPES = Object.freeze({
  POST: 'POST',
  COMMENT: 'COMMENT',
  MEMBER: 'MEMBER',
  GROUP: 'GROUP',
});

// AI작성: 로그인 회원의 콘텐츠·회원·모임 신고를 공통 API로 접수한다.
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
