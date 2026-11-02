import client from './client.js';

// AI작성: 관리자 대시보드 카드의 실제 회원 집계 값을 조회한다.
export async function getAdminDashboardSummary({ signal } = {}) {
  try {
    const response = await client.get('/admin/dashboard/summary', { signal });
    const summary = response.data?.data;
    if (!response.data?.success || !summary) {
      throw new Error(response.data?.error?.message || '대시보드 현황 응답을 확인할 수 없습니다.');
    }
    return summary;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw new Error(error.response?.data?.error?.message || '대시보드 현황을 불러오지 못했습니다.');
  }
}
