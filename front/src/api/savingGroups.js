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
    id: item.savingGroupId,
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
}

function toNullableNumber(value) {
  if (value == null) return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function toSavingGroupSettingsError(error, fallbackMessage, status) {
  const failure = new Error(error?.message || fallbackMessage);
  failure.name = 'SavingGroupSettingsError';
  failure.code = error?.code;
  failure.status = status;
  return failure;
}

export function toSavingGroupSettings(data) {
  const current = data.current;
  const pending = data.pending;

  return {
    id: data.savingGroupId,
    status: data.status,
    canAccessSettings: data.canAccessSettings,
    canEditSettings: data.canEditSettings,
    current: current == null ? null : {
      groupName: current.groupName ?? '',
      description: current.description ?? '',
      maxMembers: toNullableNumber(current.maxMembers),
      approvedMemberCount: toNullableNumber(current.approvedMemberCount),
      duesAmount: toNullableNumber(current.duesAmount),
      duesDay: toNullableNumber(current.duesDay),
    },
    pending: pending == null ? null : {
      duesAmount: toNullableNumber(pending.duesAmount),
      duesDay: toNullableNumber(pending.duesDay),
      effectiveAt: pending.effectiveAt,
      status: pending.status,
    },
    editableFields: Array.isArray(data.editableFields) ? data.editableFields : [],
  };
}

export async function getSavingGroupSettings(savingGroupId, { signal } = {}) {
  try {
    const response = await client.get(`/saving-groups/${savingGroupId}/settings`, { signal });
    if (!response.data.success || !response.data.data) {
      throw toSavingGroupSettingsError(
        response.data.error,
        '계모임 설정 정보를 불러오지 못했습니다.',
        response.status
      );
    }
    return toSavingGroupSettings(response.data.data);
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupSettingsError') {
      throw error;
    }
    throw toSavingGroupSettingsError(
      error.response?.data?.error,
      '계모임 설정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
      error.response?.status
    );
  }
}
