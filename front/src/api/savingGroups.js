import client from './client.js';

// AI작성: API 지역 코드를 화면 이름과 연결하며 DB ID로 해석하지 않는다.
export const savingGroupRegionOptions = [
  { code: '01', name: '서울' }, { code: '02', name: '경기' },
  { code: '03', name: '강원' }, { code: '04', name: '충북' },
  { code: '05', name: '충남' }, { code: '06', name: '전북' },
  { code: '07', name: '전남' }, { code: '08', name: '경북' },
  { code: '09', name: '경남' }, { code: '10', name: '제주도' },
];

// AI작성: 조회 데이터는 수정하지 않고 기존 카드의 표시용 필드로 변환한다.
export function toSavingGroupCard(item) {
  const amount = item.duesAmount == null ? null : Number(item.duesAmount);
  const dues = amount == null ? '회비 미정'
    : !Number.isSafeInteger(amount) ? '회비 확인 필요'
      : `월 ${amount.toLocaleString('ko-KR')}원`;
  return {
    id: item.groupId,
    name: item.groupName,
    description: item.description?.trim() || '소개글이 없습니다.',
    region: item.regionName,
    district: item.regionName,
    status: { '01': '모집중', '02': '진행중' }[item.status] || '상태 확인 필요',
    members: item.maxMembers == null
      ? `${item.memberCount}명 / 정원 미정` : `${item.memberCount} / ${item.maxMembers}명`,
    monthlyDues: dues,
    nextDate: item.duesDay == null ? '미정' : `매월 ${item.duesDay}일`,
  };
}

// AI작성: 회원 역할은 서버 응답만 사용하며 승인되지 않은 사용자는 공지만 표시한다.
export function getSavingGroupMenus(role) {
  const common = ['notice', 'payment', 'calendar', 'dues', 'transactionHistory', 'payoutRequest'];
  if (role === 'OWNER') return [...common, 'members', 'settings', 'joinRequests'];
  return ['MANAGER', 'MEMBER'].includes(role) ? common : ['notice'];
}

// AI작성: null을 0원으로 바꾸거나 소수 금액을 반올림하지 않고 표시 직전에 포맷한다.
export function formatSavingGroupAmount(value) {
  if (value == null) return '회비 미정';
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0) return '회비 확인 필요';
  return `${amount.toLocaleString('ko-KR', { maximumFractionDigits: 2 })}원`;
}

// AI작성: URL ID를 숫자로 변환하지 않으며 기존 API 클라이언트와 취소 신호를 재사용한다.
export async function getSavingGroupDetail(id, { signal } = {}) {
  const response = await client.get(`/saving-groups/${encodeURIComponent(id)}`, { signal });
  if (!response.data.success) {
    throw new Error(response.data.error?.message || '계모임 정보를 불러오지 못했습니다.');
  }
  return response.data.data;
}

// AI작성: 최대 다섯 개의 번호만 생성하고 빈 결과·범위 초과는 번호를 만들지 않는다.
export function getSavingGroupPages(currentPage, totalPages) {
  if (totalPages < 1 || currentPage < 1 || currentPage > totalPages) return [];
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  return Array.from({ length: Math.min(5, totalPages - start + 1) }, (_, index) => start + index);
}

// AI작성: 공통 응답 실패를 검사하고 공개 조회에는 별도 토큰을 요구하지 않는다.
export async function getSavingGroups({ keyword, regionCode, page, signal }) {
  const response = await client.get('/saving-groups', {
    params: { keyword: keyword || undefined, regionCode: regionCode || undefined, page }, signal,
  });
  if (!response.data.success) {
    throw new Error(response.data.error?.message || '목록을 불러오지 못했습니다.');
  }
  // AI작성: 공통 data/meta 응답을 기존 목록 화면의 상태 필드로 변환한다.
  const { data, meta } = response.data;
  return {
    items: data.map(toSavingGroupCard),
    totalElements: meta.total,
    totalPages: meta.totalPages,
    currentPage: meta.page,
    pageSize: meta.size,
  };
  const data = response.data.data;
  return { ...data, items: data.items.map(toSavingGroupCard) };
}
