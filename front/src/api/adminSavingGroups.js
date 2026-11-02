import client from './client.js';

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

function mapSavingGroup(group) {
  return {
    id: group.savingGroupId,
    groupId: group.groupId,
    name: group.groupName,
    description: group.description,
    region: group.regionName,
    members: group.memberCount ?? 0,
    status: formatStatus(group.status, group.adminRestricted),
    statusCode: group.status,
    adminRestricted: group.adminRestricted === true,
  };
}

// AI작성: 관리자 계모임 목록을 서버 검색·필터·페이징 조건으로 조회한다.
export async function getAdminSavingGroups({ keyword, regionCode, status, page = 1, size = 8, signal } = {}) {
  try {
    const response = await client.get('/admin/saving-groups', {
      params: {
        keyword: keyword || undefined,
        regionCode: regionCode || undefined,
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
