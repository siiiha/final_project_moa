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

// AI작성: 로그인한 사용자가 소모임 개설 전 DB에 등록된 활동 지역을 조회한다.
export async function fetchSmallGroupRegions() {
  try {
    const response = await client.get('/smallgroups/regions');
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

// AI작성: 생성자 정보는 JWT에서 서버가 결정하므로, 입력값만 본문에 담아 생성 결과를 반환한다.
export async function createSmallGroup(request) {
  try {
    const response = await client.post('/smallgroups', request);
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
