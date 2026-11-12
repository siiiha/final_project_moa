import client from './client.js';
import { formatAdminDate } from './adminFormatters.js';

function toAdminSavingGroupError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminSavingGroupError';
  failure.code = responseError?.code || 'ADMIN_SAVING_GROUP_REQUEST_FAILED';
  return failure;
}

function formatStatus(status, adminRestricted) {
  if (status === '00') return '심사중';
  if (status === '01') return '모집중';
  if (status === '02') return '활동중';
  if (status === '03') return adminRestricted ? '정지' : '일시정지';
  if (status === '04') return '종료';
  return '확인 필요';
}

function formatLeaderInfo(name, nickname) {
  if (!name && !nickname) return '-';
  if (!name) return nickname;
  if (!nickname) return name;
  return `${name}(${nickname})`;
}

function mapSavingGroup(group) {
  return {
    id: group.savingGroupId,
    groupId: group.groupId,
    name: group.groupName,
    description: group.description,
    region: group.regionName,
    leader: formatLeaderInfo(group.ownerName, group.ownerNickname),
    members: group.memberCount ?? 0,
    // AI작성: 관리자 목록에서 개설일을 일관된 날짜 형식으로 보여준다.
    openedAt: formatAdminDate(group.createdAt, { fallback: '-', compact: false }),
    status: formatStatus(group.status, group.adminRestricted),
    statusCode: group.status,
    adminRestricted: group.adminRestricted === true,
  };
}

// AI작성: 관리자 계모임 관리 화면의 전체 운영 현황을 목록 필터와 분리해 조회한다.
export async function getAdminSavingGroupSummary({ signal } = {}) {
  try {
    const response = await client.get('/admin/saving-groups/summary', { signal });
    if (!response.data?.success || !response.data.data) {
      throw new Error(response.data?.error?.message || '계모임 현황 응답을 확인할 수 없습니다.');
    }
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminSavingGroupError(error, '계모임 현황을 불러오지 못했습니다.');
  }
}

// AI작성: 관리자 계모임 목록을 서버 검색·필터·페이징 조건으로 조회한다.
export async function getAdminSavingGroups({
  keyword,
  provinceName,
  districtName,
  townName,
  status,
  page = 1,
  size = 8,
  signal,
} = {}) {
  try {
    const response = await client.get('/admin/saving-groups', {
      params: {
        keyword: keyword || undefined,
        provinceName: provinceName || undefined,
        districtName: districtName || undefined,
        townName: townName || undefined,
        status: status || undefined,
        page,
        size,
      },
      signal,
    });
    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new Error('계모임 목록 응답을 확인할 수 없습니다.');
    }
    return { items: response.data.data.map(mapSavingGroup), meta: response.data.meta };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminSavingGroupError(error, '계모임 목록을 불러오지 못했습니다.');
  }
}

export async function restrictAdminSavingGroup(savingGroupId, reason) {
  try {
    const response = await client.patch(`/admin/saving-groups/${encodeURIComponent(savingGroupId)}/restriction`, { reason });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('계모임 이용 제한 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminSavingGroupError(error, '계모임 이용 제한을 적용하지 못했습니다.');
  }
}

export async function releaseAdminSavingGroupRestriction(savingGroupId, reason) {
  try {
    const response = await client.patch(`/admin/saving-groups/${encodeURIComponent(savingGroupId)}/restriction/release`, { reason });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('계모임 이용 제한 해제 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminSavingGroupError(error, '계모임 이용 제한을 해제하지 못했습니다.');
  }
}
