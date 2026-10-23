import client from './client.js';

// AI작성: 인증 연동 전 시연용 신청자 ID이며, JWT 병합 시 서버가 로그인 회원 ID를 사용해야 한다.
const TEMP_CREATION_APPLICANT_ID = '67';

class SavingGroupCreationApplicationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SavingGroupCreationApplicationError';
    this.code = code;
  }
}

function toCreationApplicationError(error, fallbackMessage) {
  if (error instanceof SavingGroupCreationApplicationError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SavingGroupCreationApplicationError(
    responseError?.code || 'SAVING_GROUP_APPLICATION_FAILED',
    responseError?.message || fallbackMessage
  );
}

// AI작성: 화면 입력명을 서버 DTO 필드명으로 맞춘 multipart 요청을 만들고, 임시 회원 ID를 한 곳에서만 관리한다.
export async function createSavingGroupApplication(formData) {
  const requestData = new FormData();
  ['groupName', 'regionName', 'maxMembers', 'description', 'groupType', 'duesAmount', 'duesDay'].forEach((field) =>
    requestData.append(field, String(formData.get(field) || '').trim())
  );
  requestData.append('memberId', TEMP_CREATION_APPLICANT_ID);
  const image = formData.get('groupImage');
  if (image instanceof File && image.size > 0) requestData.append('groupImage', image);

  try {
    const response = await client.post('/saving-group', requestData);
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_FAILED',
        response.data?.error?.message || '계모임 개설 신청에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '계모임 개설 신청에 실패했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

function formatApplicationDate(value) {
  if (!value) return '날짜 확인 필요';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '날짜 확인 필요';
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
    .format(date)
    .replace(/\./g, '.')
    .replace(/\s/g, '');
}

// AI작성: 관리자 개설 심사 화면은 CREATE 신청만 조회하는 실제 API 결과를 화면 필드로 변환한다.
export async function getSavingGroupCreationApplications({ status, keyword, page = 1, signal } = {}) {
  try {
    const response = await client.get('/admin/saving-group-applications', {
      params: { status: status || undefined, keyword: keyword || undefined, page },
      signal,
    });
    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '심사 목록 응답을 확인하지 못했습니다.'
      );
    }
    return {
      items: response.data.data.map((item) => ({
        id: item.approvalId,
        name: item.groupName,
        applicant: item.applicantName,
        region: item.regionName,
        appliedAt: formatApplicationDate(item.appliedAt),
        status: { PENDING: '대기', APPROVED: '승인', REJECTED: '반려' }[item.status] || '상태 확인 필요',
      })),
      meta: response.data.meta,
    };
  } catch (error) {
    throw toCreationApplicationError(error, '심사 목록을 불러오지 못했습니다.');
  }
}

export async function getSavingGroupCreationApplication(approvalId, { signal } = {}) {
  try {
    const response = await client.get(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}`, { signal });
    if (!response.data?.success || !response.data.data) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '심사 상세 응답을 확인하지 못했습니다.'
      );
    }
    return response.data.data;
  } catch (error) {
    throw toCreationApplicationError(error, '심사 상세를 불러오지 못했습니다.');
  }
}

export async function approveSavingGroupCreationApplication(approvalId) {
  try {
    const response = await client.patch(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}/approve`);
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_APPROVE_FAILED',
        response.data?.error?.message || '개설 신청 승인에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '개설 신청 승인에 실패했습니다.');
  }
}

export async function rejectSavingGroupCreationApplication(approvalId, rejectReason) {
  try {
    const response = await client.patch(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}/reject`, {
      rejectReason,
    });
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_REJECT_FAILED',
        response.data?.error?.message || '개설 신청 반려에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '개설 신청 반려에 실패했습니다.');
  }
}

// AI작성: DB의 양수 BIGINT 범위만 허용하고 큰 문자열 ID를 Number로 바꾸지 않는다.
function savingGroupPathId(value) {
  if (
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9]\d*$/.test(String(value)) ||
    BigInt(value) > 9223372036854775807n
  ) {
    throw new SavingGroupJoinRequestError('BAD_REQUEST', '모임 또는 신청 번호가 올바르지 않습니다.');
  }
  return String(value);
}

// AI작성: 목록과 승인 모두 서버의 오류 코드를 보존하며 요청 취소는 화면 오류로 바꾸지 않는다.
async function savingGroupJoinResponse(request, fallback) {
  try {
    const response = await request();
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || fallback
      );
    }
    return response.data;
  } catch (error) {
    if (error instanceof SavingGroupJoinRequestError || error.code === 'ERR_CANCELED') throw error;
    throw new SavingGroupJoinRequestError(
      error.response?.data?.error?.code || 'JOIN_REQUEST_FAILED',
      error.response?.data?.error?.message || fallback
    );
  }
}

// AI작성: 실제 신청 목록만 조회한다. 토큰 제공은 기존 JWT 연결 작업으로 남기며 Mock으로 대체하지 않는다.
export async function getSavingGroupJoinRequests(savingGroupId, { status, page = 1, signal } = {}) {
  const groupId = savingGroupPathId(savingGroupId);
  const result = await savingGroupJoinResponse(
    () =>
      client.get(`/saving-groups/${groupId}/join-requests`, {
        params: { status: status || undefined, page },
        signal,
      }),
    '가입 신청 목록을 불러오지 못했습니다.'
  );
  // AI작성: 잘못된 성공 응답을 빈 목록으로 위장하거나 기술 오류 문구로 노출하지 않는다.
  if (!Array.isArray(result.data) || !result.meta || !Number.isInteger(result.meta.totalPages)) {
    throw new SavingGroupJoinRequestError(
      'INVALID_RESPONSE',
      '가입 신청 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.'
    );
  }
  return {
    items: result.data.map((item) => ({
      id: savingGroupPathId(item.crewMemberId),
      applicant: item.applicant || '이름 미등록',
      verification:
        item.identityVerified && item.regionVerified
          ? '본인·지역 인증 완료'
          : item.identityVerified
            ? '본인 인증 완료'
            : item.regionVerified
              ? '지역 인증 완료'
              : '인증 미완료',
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
  return savingGroupJoinResponse(
    () => client.patch(`/saving-groups/${groupId}/join-requests/${requestId}/approve`),
    '가입 신청 승인에 실패했습니다.'
  );
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
  { code: '01', name: '서울' },
  { code: '02', name: '경기' },
  { code: '03', name: '강원' },
  { code: '04', name: '충북' },
  { code: '05', name: '충남' },
  { code: '06', name: '전북' },
  { code: '07', name: '전남' },
  { code: '08', name: '경북' },
  { code: '09', name: '경남' },
  { code: '10', name: '제주도' },
];

// AI작성: 조회 데이터는 수정하지 않고 기존 카드의 표시용 필드로 변환한다.
export function toSavingGroupCard(item) {
  const amount = item.duesAmount == null ? null : Number(item.duesAmount);
  const dues =
    amount == null
      ? '회비 미정'
      : !Number.isSafeInteger(amount)
        ? '회비 확인 필요'
        : `월 ${amount.toLocaleString('ko-KR')}원`;
  return {
    id: item.groupId,
    name: item.groupName,
    description: item.description?.trim() || '소개글이 없습니다.',
    region: item.regionName,
    district: item.regionName,
    status: { '01': '모집중', '02': '진행중' }[item.status] || '상태 확인 필요',
    members:
      item.maxMembers == null ? `${item.memberCount}명 / 정원 미정` : `${item.memberCount} / ${item.maxMembers}명`,
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
    params: { keyword: keyword || undefined, regionCode: regionCode || undefined, page },
    signal,
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
    current:
      current == null
        ? null
        : {
            groupName: current.groupName ?? '',
            description: current.description ?? '',
            maxMembers: toNullableNumber(current.maxMembers),
            approvedMemberCount: toNullableNumber(current.approvedMemberCount),
            duesAmount: toNullableNumber(current.duesAmount),
            duesDay: toNullableNumber(current.duesDay),
          },
    pending:
      pending == null
        ? null
        : {
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
      throw toSavingGroupSettingsError(response.data.error, '계모임 설정 정보를 불러오지 못했습니다.', response.status);
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

export async function updateSavingGroupSettings(savingGroupId, changedFields) {
  try {
    const response = await client.patch(`/saving-groups/${savingGroupId}`, changedFields);
    if (!response.data.success) {
      throw toSavingGroupSettingsError(response.data.error, '계모임 설정을 저장하지 못했습니다.', response.status);
    }
    return response.data.data;
  } catch (error) {
    if (error.name === 'SavingGroupSettingsError') throw error;
    throw toSavingGroupSettingsError(
      error.response?.data?.error,
      '계모임 설정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      error.response?.status
    );
  }
}

// AI작성: 가입 신청은 별도 입력값 없이 계모임 ID만 경로에 담아 요청하고 서버 오류 코드를 화면에 전달한다.
export async function requestSavingGroupJoin(savingGroupId) {
  try {
    const response = await client.post(`/saving-groups/${savingGroupId}/join-requests`);
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || '가입 신청 처리에 실패했습니다.'
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
      responseError?.message || '가입 신청 처리에 실패했습니다.'
    );
  }
}
