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
