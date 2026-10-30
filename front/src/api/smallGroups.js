import client from './client.js';

// AI작성: 소모임 생성 API 실패 코드와 메시지를 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupCreationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupCreationError';
    this.code = code;
  }
}

// AI작성: 지역 목록 조회 실패 코드와 메시지를 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupRegionLoadError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupRegionLoadError';
    this.code = code;
  }
}

// AI작성: 목록 조회 실패 코드와 메시지를 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupListError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupListError';
    this.code = code;
  }
}

// AI작성: 소모임 상세 조회 실패 코드와 메시지를 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupDetailLoadError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupDetailLoadError';
    this.code = code;
  }
}

// AI작성: 소모임 설정 조회 실패 코드와 메시지를 설정 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupSettingsLoadError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupSettingsLoadError';
    this.code = code;
  }
}

// AI작성: 소모임 설정 수정 실패 코드와 메시지를 설정 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupSettingsUpdateError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupSettingsUpdateError';
    this.code = code;
  }
}

// AI작성: 소모임 가입 신청 실패 코드와 메시지를 상세 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupJoinRequestError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupJoinRequestError';
    this.code = code;
  }
}

// AI작성: 소모임 투표 생성 실패 코드와 메시지를 투표 생성 화면까지 보존하기 위한 오류 객체다.
export class SmallGroupVoteCreateError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupVoteCreateError';
    this.code = code;
  }
}

// AI작성: 소모임 투표 목록 조회 실패 코드와 메시지를 투표 탭까지 보존하기 위한 오류 객체다.
export class SmallGroupVoteListLoadError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupVoteListLoadError';
    this.code = code;
  }
}

// AI작성: interest 기준 카테고리 목록 조회 실패를 생성·목록 오류와 구분한다.
export class SmallGroupCategoryLoadError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SmallGroupCategoryLoadError';
    this.code = code;
  }
}

// AI작성: 백엔드 목록의 regionCode 01~10 규칙에 맞춘 광역 지역 선택지다.
export const smallGroupRegionOptions = [
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

function toSmallGroupCreationError(error, fallbackMessage) {
  if (error instanceof SmallGroupCreationError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupCreationError(
    responseError?.code || 'SMALL_GROUP_CREATION_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupRegionLoadError(error, fallbackMessage) {
  if (error instanceof SmallGroupRegionLoadError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupRegionLoadError(
    responseError?.code || 'SMALL_GROUP_REGION_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupListError(error, fallbackMessage) {
  if (error instanceof SmallGroupListError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupListError(
    responseError?.code || 'SMALL_GROUP_LIST_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupDetailLoadError(error, fallbackMessage) {
  if (error instanceof SmallGroupDetailLoadError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupDetailLoadError(
    responseError?.code || 'SMALL_GROUP_DETAIL_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupSettingsLoadError(error, fallbackMessage) {
  if (error instanceof SmallGroupSettingsLoadError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupSettingsLoadError(
    responseError?.code || 'SMALL_GROUP_SETTINGS_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupSettingsUpdateError(error, fallbackMessage) {
  if (error instanceof SmallGroupSettingsUpdateError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupSettingsUpdateError(
    responseError?.code || 'SMALL_GROUP_SETTINGS_UPDATE_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupJoinRequestError(error, fallbackMessage) {
  if (error instanceof SmallGroupJoinRequestError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupJoinRequestError(
    responseError?.code || 'JOIN_REQUEST_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupVoteCreateError(error, fallbackMessage) {
  if (error instanceof SmallGroupVoteCreateError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupVoteCreateError(
    responseError?.code || 'SMALL_GROUP_VOTE_CREATE_FAILED',
    responseError?.message || fallbackMessage,
  );
}

function toSmallGroupVoteListLoadError(error, fallbackMessage) {
  if (error instanceof SmallGroupVoteListLoadError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupVoteListLoadError(
    responseError?.code || 'SMALL_GROUP_VOTE_LIST_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

// AI작성: BIGINT 소모임·신청 식별자는 Number로 변환하지 않고 DB 양수 범위 문자열만 API 경로·화면 키로 사용한다.
function smallGroupJoinRequestPathId(value) {
  if (
    (typeof value === 'number' && !Number.isSafeInteger(value))
    || !/^[1-9]\d*$/.test(String(value))
    || BigInt(value) > 9223372036854775807n
  ) {
    throw new SmallGroupJoinRequestError('BAD_REQUEST', '소모임 또는 신청 번호가 올바르지 않습니다.');
  }
  return String(value);
}

function toSmallGroupCategoryLoadError(error, fallbackMessage) {
  if (error instanceof SmallGroupCategoryLoadError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SmallGroupCategoryLoadError(
    responseError?.code || 'SMALL_GROUP_CATEGORY_LOAD_FAILED',
    responseError?.message || fallbackMessage,
  );
}

// AI작성: 로그인한 사용자가 소모임 개설 전 DB에 등록된 활동 지역을 조회한다.
export async function fetchSmallGroupRegions() {
  try {
    const response = await client.get('/small-groups/regions');
    if (!response.data?.success || !Array.isArray(response.data.data)) {
      throw new SmallGroupRegionLoadError(
        response.data?.error?.code || 'SMALL_GROUP_REGION_LOAD_FAILED',
        response.data?.error?.message || '활동 지역을 불러오지 못했습니다.',
      );
    }
    return response.data.data;
  } catch (error) {
    throw toSmallGroupRegionLoadError(error, '활동 지역을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 목록 필터는 interest 기준 데이터만 사용하며, 소모임의 현재 목록에서 임의로 만들지 않는다.
export async function fetchSmallGroupCategories({ signal } = {}) {
  try {
    const response = await client.get('/small-groups/categories', { signal });
    if (!response.data?.success || !Array.isArray(response.data.data)) {
      throw new SmallGroupCategoryLoadError(
        response.data?.error?.code || 'SMALL_GROUP_CATEGORY_LOAD_FAILED',
        response.data?.error?.message || '관심사 목록을 불러오지 못했습니다.',
      );
    }
    return response.data.data;
  } catch (error) {
    throw toSmallGroupCategoryLoadError(error, '관심사 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 서버 목록 필드를 기존 카드의 표시 필드로만 변환하고 원본 정렬·페이지 순서는 바꾸지 않는다.
export function toSmallGroupCard(item) {
  const memberCount = Number.isFinite(Number(item.memberCount)) ? Number(item.memberCount) : 0;
  return {
    // AI작성: 상세 API의 경로 변수는 groups.group_id가 아닌 small_group.small_group_id를 사용한다.
    id: item.smallGroupId,
    name: item.groupName,
    region: item.regionName,
    interest: item.likeCategory?.trim() || '관심사 미정',
    participants: item.maxMembers == null
      ? `${memberCount}명 / 정원 미정`
      : `${memberCount} / ${item.maxMembers}명`,
    description: item.description?.trim() || '소개글이 없습니다.',
  };
}

// AI작성: 계모임 목록과 동일하게 최대 다섯 개의 페이지 번호만 표시한다.
export function getSmallGroupPages(currentPage, totalPages) {
  if (totalPages < 1 || currentPage < 1 || currentPage > totalPages) return [];
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  return Array.from({ length: Math.min(5, totalPages - start + 1) }, (_, index) => start + index);
}

// AI작성: 서버가 검색·필터·페이징을 처리하므로 프론트는 요청 조건과 응답 유효성만 관리한다.
export async function getSmallGroups({ keyword, regionCode, likeCategory, page = 1, signal } = {}) {
  try {
    const response = await client.get('/small-groups', {
      params: {
        keyword: keyword || undefined,
        regionCode: regionCode || undefined,
        likeCategory: likeCategory || undefined,
        page,
      },
      signal,
    });
    const { data, meta } = response.data || {};
    if (!response.data?.success || !Array.isArray(data) || !meta
      || !Number.isInteger(meta.page) || !Number.isInteger(meta.totalPages)) {
      throw new SmallGroupListError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '소모임 목록 응답을 확인하지 못했습니다.',
      );
    }
    return {
      items: data.map(toSmallGroupCard),
      totalElements: meta.total,
      totalPages: meta.totalPages,
      currentPage: meta.page,
      pageSize: meta.size,
    };
  } catch (error) {
    throw toSmallGroupListError(error, '소모임 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 공지 본문 등 목록 화면에 불필요한 데이터는 전달하지 않고 제목·작성자·작성일만 보존한다.
export function toSmallGroupNoticeSummary(notice) {
  const hasNoticeId = typeof notice?.noticeId === 'number' || typeof notice?.noticeId === 'string';
  if (!hasNoticeId || typeof notice.title !== 'string'
    || typeof notice.writerNickname !== 'string' || typeof notice.createdAt !== 'string') {
    throw new SmallGroupDetailLoadError('INVALID_RESPONSE', '소모임 최근 공지 응답을 확인하지 못했습니다.');
  }
  return {
    noticeId: notice.noticeId,
    title: notice.title,
    writerNickname: notice.writerNickname,
    createdAt: notice.createdAt,
  };
}

// AI작성: 공개 상세 API는 로그인 여부와 관계없이 응답 봉투와 최근 공지 요약 배열을 확인한다.
export function toSmallGroupDetail(data) {
  if (!data || !Array.isArray(data.notices)) {
    throw new SmallGroupDetailLoadError('INVALID_RESPONSE', '소모임 상세 응답을 확인하지 못했습니다.');
  }
  return {
    ...data,
    notices: data.notices.map(toSmallGroupNoticeSummary),
  };
}

export async function getSmallGroupDetail(id, { signal } = {}) {
  try {
    const response = await client.get(`/small-groups/${encodeURIComponent(id)}`, { signal });
    if (!response.data?.success || !response.data.data) {
      throw new SmallGroupDetailLoadError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '소모임 상세 응답을 확인하지 못했습니다.',
      );
    }
    return toSmallGroupDetail(response.data.data);
  } catch (error) {
    throw toSmallGroupDetailLoadError(error, '소모임 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 가입 신청은 별도 입력값 없이 소모임 ID만 경로에 담아 요청하고 서버 오류 코드를 화면에 전달한다.
export async function requestSmallGroupJoin(smallGroupId) {
  try {
    const response = await client.post(`/small-groups/${encodeURIComponent(smallGroupId)}/join-requests`);
    if (!response.data?.success) {
      throw new SmallGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || '가입 신청 처리에 실패했습니다.',
      );
    }
    return response.data;
  } catch (error) {
    throw toSmallGroupJoinRequestError(error, '가입 신청 처리에 실패했습니다.');
  }
}

// AI작성: 투표 생성자는 JWT에서 서버가 결정하므로 소모임 ID와 화면 입력값만 전송한다.
export async function createSmallGroupVote(smallGroupId, request) {
  try {
    const response = await client.post(
      `/small-groups/${encodeURIComponent(smallGroupId)}/votes`,
      request,
    );
    if (!response.data?.success || response.data.data?.smallGroupVoteId == null) {
      throw new SmallGroupVoteCreateError(
        response.data?.error?.code || 'SMALL_GROUP_VOTE_CREATE_FAILED',
        response.data?.error?.message || '투표 생성 응답을 확인하지 못했습니다.',
      );
    }
    return response.data.data;
  } catch (error) {
    throw toSmallGroupVoteCreateError(error, '투표를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 투표 목록은 제목·OPEN/CLOSED 상태·마감 시각만 사용하며 마감 시각 미설정은 null로 보존한다.
export function toSmallGroupVoteListItem(item) {
  const hasValidEndsAt = item?.endsAt == null || typeof item.endsAt === 'string';
  const hasValidStatus = ['OPEN', 'CLOSED'].includes(item?.status);
  if (typeof item?.title !== 'string' || !hasValidStatus || !hasValidEndsAt) {
    throw new SmallGroupVoteListLoadError('INVALID_RESPONSE', '투표 목록 응답을 확인하지 못했습니다.');
  }
  return {
    title: item.title,
    status: item.status,
    endsAt: item.endsAt,
  };
}

// AI작성: 투표 목록은 서버의 소속 권한·정렬 결과를 그대로 사용하며 검색·필터·페이징을 추가하지 않는다.
export async function getSmallGroupVotes(smallGroupId, { signal } = {}) {
  try {
    const response = await client.get(
      `/small-groups/${encodeURIComponent(smallGroupId)}/votes`,
      { signal },
    );
    if (!response.data?.success || !Array.isArray(response.data.data)) {
      throw new SmallGroupVoteListLoadError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '투표 목록 응답을 확인하지 못했습니다.',
      );
    }
    return response.data.data.map(toSmallGroupVoteListItem);
  } catch (error) {
    throw toSmallGroupVoteListLoadError(error, '투표 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 개설자 전용 목록은 서버의 상태·페이지 결과를 그대로 사용하며 Mock 데이터나 클라이언트 재정렬을 적용하지 않는다.
export async function getSmallGroupJoinRequests(smallGroupId, { status, page = 1, signal } = {}) {
  const groupId = smallGroupJoinRequestPathId(smallGroupId);
  try {
    const response = await client.get(`/small-groups/${groupId}/join-requests`, {
      params: { status: status || undefined, page },
      signal,
    });
    const { data, meta } = response.data || {};
    if (!response.data?.success || !Array.isArray(data) || !meta || !Number.isInteger(meta.totalPages)) {
      throw new SmallGroupJoinRequestError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '가입 신청 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.',
      );
    }
    return {
      items: data.map((item) => ({
        id: smallGroupJoinRequestPathId(item.crewMemberId),
        applicant: item.applicant || '이름 미등록',
        regionInterest: item.regionName || '지역 미등록',
        message: item.applicationMessage || '신청 메시지가 없습니다.',
        status: { PENDING: '대기', APPROVED: '승인', REJECTED: '거절' }[item.membershipStatus] || '상태 확인 필요',
      })),
      meta,
    };
  } catch (error) {
    throw toSmallGroupJoinRequestError(error, '가입 신청 목록을 불러오지 못했습니다.');
  }
}

// AI작성: 개설자 승인 요청은 소모임·신청 ID를 경로로만 전달하고 요청 본문이나 처리자 ID를 보내지 않는다.
export async function approveSmallGroupJoinRequest(smallGroupId, crewMemberId) {
  const groupId = smallGroupJoinRequestPathId(smallGroupId);
  const requestId = smallGroupJoinRequestPathId(crewMemberId);
  try {
    const response = await client.patch(
      `/small-groups/${groupId}/join-requests/${requestId}/approve`,
    );
    if (!response.data?.success) {
      throw new SmallGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_APPROVAL_FAILED',
        response.data?.error?.message || '가입 신청을 승인하지 못했습니다.',
      );
    }
    return response.data;
  } catch (error) {
    throw toSmallGroupJoinRequestError(error, '가입 신청을 승인하지 못했습니다.');
  }
}

// AI작성: 개설자 거절 요청은 소모임·신청 ID만 경로로 전달하고 요청 본문이나 처리자 ID를 보내지 않는다.
export async function rejectSmallGroupJoinRequest(smallGroupId, crewMemberId) {
  const groupId = smallGroupJoinRequestPathId(smallGroupId);
  const requestId = smallGroupJoinRequestPathId(crewMemberId);
  try {
    const response = await client.patch(
      `/small-groups/${groupId}/join-requests/${requestId}/reject`,
    );
    if (!response.data?.success) {
      throw new SmallGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_REJECTION_FAILED',
        response.data?.error?.message || '가입 신청을 거절하지 못했습니다.',
      );
    }
    return response.data;
  } catch (error) {
    throw toSmallGroupJoinRequestError(error, '가입 신청을 거절하지 못했습니다.');
  }
}

// AI작성: OWNER 설정 화면은 수정 가능 필드와 승인 참여 인원 수가 모두 있어야만 폼을 초기화한다.
export function toSmallGroupSettings(data) {
  const hasValidCapacity = Number.isInteger(data?.maxMembers)
    && data.maxMembers >= 2 && data.maxMembers <= 20;
  const hasValidApprovedMemberCount = Number.isInteger(data?.approvedMemberCount)
    && data.approvedMemberCount >= 0;
  if (typeof data?.groupName !== 'string' || typeof data.likeCategory !== 'string'
    || typeof data.description !== 'string' || !hasValidCapacity
    || typeof data.meetDate !== 'string' || typeof data.location !== 'string'
    || !hasValidApprovedMemberCount) {
    throw new SmallGroupSettingsLoadError('INVALID_RESPONSE', '소모임 설정 응답을 확인하지 못했습니다.');
  }
  return data;
}

// AI작성: 설정 조회는 공개 상세와 달리 인증이 필요한 OWNER 전용 API를 호출한다.
export async function getSmallGroupSettings(id, { signal } = {}) {
  try {
    const response = await client.get(`/small-groups/${encodeURIComponent(id)}/settings`, { signal });
    if (!response.data?.success || !response.data.data) {
      throw new SmallGroupSettingsLoadError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '소모임 설정 응답을 확인하지 못했습니다.',
      );
    }
    return toSmallGroupSettings(response.data.data);
  } catch (error) {
    throw toSmallGroupSettingsLoadError(error, '소모임 설정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 화면이 비교해 만든 변경 필드만 OWNER 전용 PATCH 본문으로 전달하고 성공 봉투만 확인한다.
export async function updateSmallGroupSettings(id, changes) {
  try {
    const response = await client.patch(`/small-groups/${encodeURIComponent(id)}`, changes);
    if (!response.data?.success) {
      throw new SmallGroupSettingsUpdateError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '소모임 설정 수정 응답을 확인하지 못했습니다.',
      );
    }
  } catch (error) {
    throw toSmallGroupSettingsUpdateError(error, '소모임 설정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

// AI작성: 생성자 정보는 JWT에서 서버가 결정하므로, 입력값만 본문에 담아 생성 결과를 반환한다.
export async function createSmallGroup(request) {
  try {
    const response = await client.post('/small-groups', request);
    if (!response.data?.success || !response.data.data) {
      throw new SmallGroupCreationError(
        response.data?.error?.code || 'SMALL_GROUP_CREATION_FAILED',
        response.data?.error?.message || '소모임 생성에 실패했습니다.',
      );
    }
    return response.data.data;
  } catch (error) {
    throw toSmallGroupCreationError(error, '소모임 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.');
  }
}
