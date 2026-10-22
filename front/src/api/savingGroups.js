import client from './client.js';

// AI작성: DB의 양수 BIGINT 범위만 허용하고 큰 문자열 ID를 Number로 바꾸지 않는다.
function savingGroupPathId(value) {
  if ((typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9]\d*$/.test(String(value)) || BigInt(value) > 9223372036854775807n) {
    throw new SavingGroupJoinRequestError('BAD_REQUEST', '모임 또는 신청 번호가 올바르지 않습니다.');
  }
  return String(value);
}

// AI작성: 목록과 승인 모두 서버의 오류 코드를 보존하며 요청 취소는 화면 오류로 바꾸지 않는다.
async function savingGroupJoinResponse(request, fallback) {
  try {
    const response = await request();
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(response.data?.error?.code || 'JOIN_REQUEST_FAILED', response.data?.error?.message || fallback);
    }
    return response.data;
  } catch (error) {
    if (error instanceof SavingGroupJoinRequestError || error.code === 'ERR_CANCELED') throw error;
    throw new SavingGroupJoinRequestError(error.response?.data?.error?.code || 'JOIN_REQUEST_FAILED',
      error.response?.data?.error?.message || fallback);
  }
}

// AI작성: 실제 신청 목록만 조회한다. 토큰 제공은 기존 JWT 연결 작업으로 남기며 Mock으로 대체하지 않는다.
export async function getSavingGroupJoinRequests(savingGroupId, { status, page = 1, signal } = {}) {
  const groupId = savingGroupPathId(savingGroupId);
  const result = await savingGroupJoinResponse(() => client.get(`/saving-groups/${groupId}/join-requests`, {
    params: { status: status || undefined, page }, signal,
  }), '가입 신청 목록을 불러오지 못했습니다.');
  // AI작성: 잘못된 성공 응답을 빈 목록으로 위장하거나 기술 오류 문구로 노출하지 않는다.
  if (!Array.isArray(result.data) || !result.meta || !Number.isInteger(result.meta.totalPages)) {
    throw new SavingGroupJoinRequestError('INVALID_RESPONSE', '가입 신청 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.');
  }
  return {
    items: result.data.map((item) => ({
      id: savingGroupPathId(item.crewMemberId),
      applicant: item.applicant || '이름 미등록',
      verification: item.identityVerified && item.regionVerified ? '본인·지역 인증 완료'
        : item.identityVerified ? '본인 인증 완료' : item.regionVerified ? '지역 인증 완료' : '인증 미완료',
      regionInterest: item.regionName || '지역 미등록',
      message: item.applicationMessage || '신청 메시지가 없습니다.',
      status: { PENDING: '대기', APPROVED: '승인', REJECTED: '거절' }[item.membershipStatus] || '상태 확인 필요',
    })),
    meta: result.meta,
  };
}

// AI작성: 승인자·역할·본문을 보내지 않고 서버가 반환한 실제 신청 ID만 사용한다.
export async function approveSavingGroupJoin(savingGroupId, crewMemberId) {
  const groupId = savingGroupPathId(savingGroupId);
  const requestId = savingGroupPathId(crewMemberId);
  return savingGroupJoinResponse(() => client.patch(`/saving-groups/${groupId}/join-requests/${requestId}/approve`),
    '가입 신청 승인에 실패했습니다.');
}

class SavingGroupJoinRequestError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SavingGroupJoinRequestError';
    this.code = code;
  }
}

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

// AI작성: 가입 신청은 별도 입력값 없이 계모임 ID만 경로에 담아 요청하고 서버 오류 코드를 화면에 전달한다.
export async function requestSavingGroupJoin(savingGroupId) {
  try {
    const response = await client.post(`/saving-groups/${savingGroupId}/join-requests`);
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || '가입 신청 처리에 실패했습니다.',
      );
    }
    return response.data;
  } catch (error) {
    if (error instanceof SavingGroupJoinRequestError) {
      throw error;
    }
    const responseError = error.response?.data?.error;
    throw new SavingGroupJoinRequestError(
      responseError?.code || 'JOIN_REQUEST_FAILED',
      responseError?.message || '가입 신청 처리에 실패했습니다.',
    );
  }
}
